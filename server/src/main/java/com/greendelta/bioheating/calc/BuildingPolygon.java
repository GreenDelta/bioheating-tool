package com.greendelta.bioheating.calc;

import java.util.Objects;

import org.locationtech.jts.geom.Polygon;

import com.greendelta.bioheating.model.Building;

public record BuildingPolygon(Building building, Polygon polygon) {

	public BuildingPolygon {
		Objects.requireNonNull(building);
		Objects.requireNonNull(polygon);
	}
}
