package com.greendelta.bioheating.io;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.locationtech.jts.geom.Coordinate;

import com.fasterxml.jackson.databind.node.ObjectNode;

public record OsmStreet(ObjectNode object) {

	public OsmStreet {
		Objects.requireNonNull(object);
	}

	public String type() {
		var t = object.get("type");
		if (t == null)
			return null;
		return t.isTextual()
			? t.asText()
			: null;
	}

	public long id() {
		var id = object.get("id");
		return id != null && id.isNumber()
			? id.asLong()
			: 0;
	}

	public Map<String, String> tags() {
		var tags = object.get("tags");
		if (tags == null || !tags.isObject())
			return Map.of();
		var map = new HashMap<String, String>();
		for (var props : tags.properties()) {
			var key = props.getKey();
			var val = props.getValue();
			if (val != null && val.isTextual()) {
				map.put(key, val.asText());
			}
		}
		return map;
	}

	public List<Coordinate> geometry() {
		var geometry = object.get("geometry");
		if (geometry == null || !geometry.isArray())
			return List.of();
		var coordinates = new ArrayList<Coordinate>();
		for (var point : geometry) {
			if (point.has("lat") && point.has("lon")) {
				double lat = point.get("lat").asDouble();
				double lon = point.get("lon").asDouble();
				coordinates.add(new Coordinate(lon, lat));
			}
		}
		return coordinates;
	}

}
