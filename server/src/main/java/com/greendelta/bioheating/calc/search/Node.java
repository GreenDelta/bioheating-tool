package com.greendelta.bioheating.calc.search;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import com.greendelta.bioheating.model.Building;

public record Node(
	Building building, Polygon polygon, Point center
) {

	public long id() {
		return building.id();
	}

	public static Node of(Building building, GeometryFactory f) {
		var polygon = f.createPolygon(building.coordinates());
		var center = polygon.getCentroid();
		return new Node(building, polygon, center);
	}

}
