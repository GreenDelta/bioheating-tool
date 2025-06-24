package com.greendelta.bioheating.io;

import org.locationtech.jts.geom.Coordinate;

import com.greendelta.bioheating.model.GeoMap;
import com.greendelta.bioheating.model.Street;
import com.greendelta.bioheating.util.Res;

class OsmStreetFetch {

	private final GeoMap map;
	private final OsmClient client;

	private OsmStreetFetch(GeoMap map) {
		this.map = map;
		this.client = OsmClient.getDefault();
	}

	public static Res<Void> into(GeoMap map) {
		return map == null || map.buildings().isEmpty()
			? Res.error("no buildings found")
			: new OsmStreetFetch(map).doIt();
	}

	private Res<Void> doIt() {

		// calculate the map bounds
		var boundsRes = Bounds.of(map);
		if (boundsRes.hasError())
			return boundsRes.wrapError("failed to get map bound");
		var bs = boundsRes.value();

		// fetch the streets within these bounds
		var streets = client.queryStreets(bs.south, bs.west, bs.north, bs.east);
		if (streets.hasError())
			return streets.wrapError("failed to fetch streets");

		// initialize the projector
		var transRes = CoordinateTransformer.fromWgs84To(map.crs());
		if (transRes.hasError())
			return transRes.wrapError("failed to load projector");
		var trans = transRes.value();

		// create the streets
		for (var s : streets.value()) {
			var geometry = s.geometry();
			if (geometry == null || geometry.isEmpty())
				continue;

			var cs = new Coordinate[geometry.size()];
			for (int i = 0; i < geometry.size(); i++) {
				var osmCoord = geometry.get(i);
				cs[i] = new Coordinate(osmCoord.lon(), osmCoord.lat());
			}

			var transformed = trans.transform(cs);
			if (transformed.hasError()) {
				continue;  // TODO stop when a single street failed?
			}

			var streetName = s.tags() != null
				? s.tags().get("name")
				: null;
			if (streetName == null) {
				streetName = "Unnamed path " + s.id();
			}

			var street = new Street()
				.name(streetName)
				.coordinates(transformed.value());

			map.streets().add(street);
		}

		return Res.of(null);
	}


	private record Bounds(
		double south, double west, double north, double east
	) {

		static Res<Bounds> of(GeoMap map) {
			if (map == null || map.buildings().isEmpty())
				return Res.error("no buildings to calculate bound from");

			double minX = Double.MAX_VALUE;
			double minY = Double.MAX_VALUE;
			double maxX = -Double.MIN_VALUE;
			double maxY = -Double.MIN_VALUE;
			boolean updated = false;

			for (var building : map.buildings()) {
				var cs = building.coordinates();
				if (cs == null)
					continue;
				for (var c : cs) {
					minX = Math.min(minX, c.x);
					minY = Math.min(minY, c.y);
					maxX = Math.max(maxX, c.x);
					maxY = Math.max(maxY, c.y);
					updated = true;
				}
			}

			if (!updated)
				return Res.error("no coordinates found for buildings in map");

			var transRes = CoordinateTransformer.toWgs84From(map);
			if (transRes.hasError())
				return transRes.wrapError("could not create CRS converter");
			var trans = transRes.value();

			// add a 50 m buffer on each side
			var minRes = trans.project(minX - 50, minY - 50);
			if (minRes.hasError())
				return minRes.wrapError("bounds transform failed");
			var southWest = minRes.value();

			var maxRes = trans.project(maxX + 50, maxY + 50);
			if (maxRes.hasError())
				return maxRes.wrapError("bounds transform failed");
			var northEast = maxRes.value();

			return Res.of(new Bounds(
				southWest.y, southWest.x, northEast.y, northEast.x));
		}

	}
}
