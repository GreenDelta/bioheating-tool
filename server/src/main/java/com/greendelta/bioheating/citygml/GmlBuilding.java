package com.greendelta.bioheating.citygml;

import java.util.HashMap;
import java.util.Map;

import org.citygml4j.core.model.building.Building;
import org.locationtech.jts.geom.Polygon;

public record GmlBuilding(
	String id,
	GmlAddress address,
	Polygon groundSurface,
	double height,
	int storeys,
	String function,
	String roofType,
	Map<String, String> attributes
) {

	static GmlBuilding of(Building b, GroundSurfaceReader surfaceReader) {
		var groundSurface = surfaceReader.read(b).orElse(null);
		return new GmlBuilding(
			b.getId(),
			GmlAddress.of(b),
			groundSurface,
			heightOf(b),
			storeysOf(b),
			CityGML.firstStringOf(b.getFunctions()),
			CityGML.stringOf(b.getRoofType()),
			attributesOf(b)
		);
	}

	private static HashMap<String, String> attributesOf(Building b) {
		var attributes = new HashMap<String, String>();
		for (var a : b.getGenericAttributes()) {
			var obj = a.getObject();
			if (obj == null)
				continue;
			var name = obj.getName();
			if (name != null && obj.getValue() instanceof String value) {
				attributes.put(name, value);
			}
		}
		return attributes;
	}

	private static double heightOf(Building b) {
		var heights = b.getHeights();
		if (heights == null || heights.isEmpty())
			return 0.0;
		int n = 0;
		double sum = 0.0;
		for (var h : heights) {
			var obj = h.getObject();
			if (obj == null || obj.getValue() == null)
				continue;
			var v = obj.getValue().getValue();
			if (v == null)
				continue;
			n += 1;
			sum += v;
		}
		return switch (n) {
			case 0 -> 0.0;
			case 1 -> sum;
			default -> sum / n;
		};
	}

	private static int storeysOf(Building b) {
		var s = b.getStoreysAboveGround();
		return s != null ? s : 1;
	}
}
