package com.greendelta.bioheating.io.citygml;

import org.openlca.commons.Res;

import com.greendelta.bioheating.io.CoordinateTransformer;
import com.greendelta.bioheating.model.GeoMap;

/// The south-west and north-east coordinates (in WGS 84) of a rectangle for
/// which we want to query the streets from Open-Street-Map (OSM).
public record OsmBounds(double south, double west, double north, double east) {
	/// Calculates the query bounds for the buildings in the given map. It is
	/// the most south-east point -50m and north-east point +50m of the buildings
	/// translated from UTM to WGS 84.
	static Res<OsmBounds> of(GeoMap map) {
		if (map == null || map.buildings().isEmpty()) return Res.error(
			"no buildings to calculate bound from"
		);

		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = -Double.MIN_VALUE;
		double maxY = -Double.MIN_VALUE;
		boolean updated = false;

		for (var building : map.buildings()) {
			var cs = building.coordinates();
			if (cs == null) continue;
			for (var c : cs) {
				minX = Math.min(minX, c.x);
				minY = Math.min(minY, c.y);
				maxX = Math.max(maxX, c.x);
				maxY = Math.max(maxY, c.y);
				updated = true;
			}
		}

		if (!updated) return Res.error("no coordinates found for buildings in map");

		var transRes = CoordinateTransformer.toWgs84From(map);
		if (transRes.isError()) return transRes.wrapError(
			"could not create CRS converter"
		);
		var trans = transRes.value();

		// add a 50 m buffer on each side
		var minRes = trans.project(minX - 50, minY - 50);
		if (minRes.isError()) return minRes.wrapError("bounds transform failed");
		var southWest = minRes.value();

		var maxRes = trans.project(maxX + 50, maxY + 50);
		if (maxRes.isError()) return maxRes.wrapError("bounds transform failed");
		var northEast = maxRes.value();

		return Res.ok(
			new OsmBounds(southWest.y, southWest.x, northEast.y, northEast.x)
		);
	}

	@Override
	public String toString() {
		return (
			"Bounds [SW=(" +
			south +
			", " +
			west +
			"), NE=(" +
			north +
			", " +
			east +
			")]"
		);
	}
}
