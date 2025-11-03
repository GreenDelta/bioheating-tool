package com.greendelta.bioheating.io.citygml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.locationtech.jts.geom.Envelope;

import com.greendelta.bioheating.citygml.GmlBuilding;

/// The building shape with which we can determine the building type.
record BuildingShape(
	GmlBuilding gml,
	Envelope envelope,
	double groundArea,
	double blockVolume,
	Set<String> neighbors) {

	static List<BuildingShape> allOf(List<GmlBuilding> gmls) {
		if (gmls == null || gmls.isEmpty())
			return List.of();
		var shapes = new ArrayList<BuildingShape>(gmls.size());
		for (var gml : gmls) {
			if (gml.groundSurface() == null)
				continue;
			shapes.add(of(gml));
		}
		return shapes;
	}

	private static BuildingShape of(GmlBuilding gml) {
		double groundArea = gml.groundSurface().getArea();
		double blockVolume = gml.height() * groundArea;
		var envelope = gml.groundSurface().getEnvelopeInternal();
		return new BuildingShape(
			gml, envelope, groundArea, blockVolume, new HashSet<>());
	}

	String id() {
		return gml.id();
	}

	int neighborCount() {
		return neighbors.size();
	}

	double height() {
		return gml.height();
	}

	@Override
	public final int hashCode() {
		var id = gml.id();
		return id != null ? id.hashCode() : gml.hashCode();
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		return obj instanceof BuildingShape other
			&& Objects.equals(this.id(), other.id());
	}
}
