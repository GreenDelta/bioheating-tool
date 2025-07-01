package com.greendelta.bioheating.model.client;

import java.util.Map;

public record GeoFeature(
	String type, Geometry geometry, Map<String, Object> properties
) {
}
