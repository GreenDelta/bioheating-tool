package com.greendelta.bioheating.calc.search;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

import com.greendelta.bioheating.model.Street;

public record StreetSegment(
	Street street, LineString line, double length
) {

	public static List<StreetSegment> allOf(Street street, GeometryFactory f) {
		var cs = street.coordinates();
		if (cs.length < 2)
			return List.of();
		var segments = new ArrayList<StreetSegment>();
		for (int i = 1; i < cs.length; i++) {
			var line = f.createLineString(new Coordinate[]{
				cs[i - 1], cs[i]
			});
			segments.add(new StreetSegment(
				street,
				line,
				line.getLength()));
		}
		return segments;
	}
}
