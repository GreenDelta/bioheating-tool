package com.greendelta.bioheating.calc.search;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import com.greendelta.bioheating.model.Building;

public record BuildingShape(
	Building building, Polygon polygon, Envelope envelope, Point center
) {

	public long id() {
		return building.id();
	}

	public static BuildingShape of(Building building, GeometryFactory f) {
		var polygon = f.createPolygon(building.coordinates());
		var center = polygon.getCentroid();
		var envelope = polygon.getEnvelopeInternal();
		return new BuildingShape(building, polygon, envelope, center);
	}

}
