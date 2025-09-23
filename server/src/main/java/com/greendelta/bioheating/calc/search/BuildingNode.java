package com.greendelta.bioheating.calc.search;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import com.greendelta.bioheating.model.Building;

public record BuildingNode(
	Building building, Polygon polygon, Envelope envelope, Point center
) {

	public long id() {
		return building.id();
	}

	public static BuildingNode of(Building building, GeometryFactory f) {
		var polygon = f.createPolygon(building.coordinates());
		var center = polygon.getCentroid();
		var envelope = polygon.getEnvelopeInternal();
		return new BuildingNode(building, polygon, envelope, center);
	}

}
