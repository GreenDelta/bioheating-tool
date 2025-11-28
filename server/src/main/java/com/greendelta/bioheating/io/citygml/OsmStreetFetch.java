package com.greendelta.bioheating.io.citygml;

import org.locationtech.jts.geom.Coordinate;
import org.openlca.commons.Res;

import com.greendelta.bioheating.io.CoordinateTransformer;
import com.greendelta.bioheating.model.GeoMap;
import com.greendelta.bioheating.model.Inclusion;
import com.greendelta.bioheating.model.Street;

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
		var boundsRes = OsmBounds.of(map);
		if (boundsRes.isError())
			return boundsRes.wrapError("failed to get map bound");

		// fetch the streets within these bounds
		var streets = client.queryStreets(boundsRes.value());
		if (streets.isError())
			return streets.wrapError("failed to fetch streets");

		// initialize the projector
		var transRes = CoordinateTransformer.fromWgs84To(map.crs());
		if (transRes.isError())
			return transRes.wrapError("failed to load projector");
		var trans = transRes.value();

		// create the streets
		for (var s : streets.value()) {
			var geometry = s.geometry();
			if (geometry == null || geometry.isEmpty())
				continue;
			var street = convert(s, trans);
			if (street.isError())
				continue;  // we just skip conversion errors currently
			map.streets().add(street.value());
		}

		return Res.ok();
	}

	private Res<Street> convert(OsmStreet s, CoordinateTransformer trans) {
		if (s == null || s.geometry() == null || s.geometry().size() < 2)
			return Res.error("not a street with valid geometry");
		var geometry = s.geometry();
		var cs = new Coordinate[geometry.size()];
		for (int i = 0; i < geometry.size(); i++) {
			var osmCoord = geometry.get(i);
			cs[i] = new Coordinate(osmCoord.lon(), osmCoord.lat());
		}
		var transformed = trans.transform(cs);
		if (transformed.isError())
			return transformed.castError();

		var name = s.tags() != null
			? s.tags().get("name")
			: null;
		if (name == null) {
			name = "Unnamed path " + s.id();
		}

		var street = new Street()
			.name(name)
			.inclusion(Inclusion.OPTIONAL)
			.coordinates(transformed.value());
		return Res.ok(street);
	}

}
