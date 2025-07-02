package com.greendelta.bioheating.model.client;

import java.util.HashMap;
import java.util.Map;

import com.greendelta.bioheating.io.CoordinateTransformer;
import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.Street;
import com.greendelta.bioheating.util.Res;

public record GeoFeature(
	String type, Geometry geometry, Map<String, Object> properties
) {

	static Res<GeoFeature> of(Building b, CoordinateTransformer wgs84) {
		if (b == null || wgs84 == null)
			return Res.error("no building or coordinate transformer");
		var cs = wgs84.transform(b.coordinates());
		if (cs.hasError())
			return cs.wrapError("failed to project coordinates of building: " + b);
		var polygon = Geometry.polygonOf(cs.value());
		var props = new HashMap<String, Object>();
		props.put("@type", "building");
		props.put("id", b.id());
		props.put("name", b.name());
		props.put("roofType", b.roofType());
		props.put("function", b.function());
		props.put("height", b.height());
		props.put("storeys", b.storeys());
		props.put("groundArea", b.groundArea());
		props.put("heatedArea", b.heatedArea());
		props.put("volume", b.volume());
		props.put("country", b.country());
		props.put("locality", b.locality());
		props.put("postalCode", b.postalCode());
		props.put("street", b.street());
		props.put("streetNumber", b.streetNumber());
		props.put("climateZone", b.climateZone());
		props.put("heatDemand", b.heatDemand());
		props.put("isHeated", b.isHeated());
		props.put("inclusion", b.inclusion());
		return Res.of(new GeoFeature("Feature", polygon, props));
	}

	static Res<GeoFeature> of(Street s, CoordinateTransformer wgs84) {
		if (s == null || wgs84 == null)
			return Res.error("no street or coordinate transformer");
		var cs = wgs84.transform(s.coordinates());
		if (cs.hasError())
			return cs.wrapError("failed to project coordinates of street: " + s);
		var line = Geometry.lineOf(cs.value());
		var props = new HashMap<String, Object>();
		props.put("@type", "street");
		props.put("id", s.id());
		props.put("name", s.name());
		props.put("inclusion", s.inclusion());
		return Res.of(new GeoFeature("Feature", line, props));
	}
}
