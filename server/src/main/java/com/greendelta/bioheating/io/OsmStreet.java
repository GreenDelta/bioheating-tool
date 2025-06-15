package com.greendelta.bioheating.io;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greendelta.bioheating.util.Res;

public record OsmStreet(
    String type,
    long id,
    OsmBounds bounds,
    List<Long> nodes,
    List<OsmCoordinate> geometry,
    Map<String, String> tags
) {

	public static Res<List<OsmStreet>> allFrom(
		JsonNode array, ObjectMapper mapper
	) {
		if (array == null || !array.isArray())
			return Res.error("provided JSON is not an array");

		var streets = new ArrayList<OsmStreet>(array.size());
		for (var e : array) {
			try {
				var street = mapper.treeToValue(e, OsmStreet.class);
				streets.add(street);
			} catch (Exception ex) {
				return Res.error("failed to parse street");
			}
		}
		return Res.of(streets);
	}

	public record OsmBounds(
		double minlat,
		double minlon,
		double maxlat,
		double maxlon
	) {
	}

	public record OsmCoordinate(
		double lat,
		double lon
	) {
	}
}
