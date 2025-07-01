package com.greendelta.bioheating.model.client;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;

public sealed interface Geometry {

	static GeoPolygon polygonOf(Coordinate[] cs) {
		var ring = new ArrayList<List<Double>>(cs.length);
		for (var c : cs) {
			ring.add(List.of(c.x, c.y));
		}
		return new GeoPolygon("Polygon", List.of(ring));
	}

	static GeoLine lineOf(Coordinate[] cs) {
		var line = new ArrayList<List<Double>>(cs.length);
		for (var c : cs) {
			line.add(List.of(c.x, c.y));
		}
		return new GeoLine("LineString", line);
	}

	record GeoPolygon(String type, List<List<List<Double>>> coordinates)
		implements Geometry {
	}

	record GeoLine(String type, List<List<Double>> coordinates)
		implements Geometry {
	}
}
