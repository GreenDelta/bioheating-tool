package com.greendelta.bioheating.io.citygml;

import java.util.HashSet;
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

	static BuildingShape of(GmlBuilding gml) {
		if (gml == null || gml.groundSurface() == null)
			return new BuildingShape(gml, null, 0, 0, null);
		double groundArea = gml.groundSurface().getArea();
		double blockVolume = gml.height() * groundArea;
		var envelope = gml.groundSurface().getEnvelopeInternal();
		return new BuildingShape(
			gml, envelope, groundArea, blockVolume, new HashSet<>());
	}

	boolean isEmpty() {
		return gml == null || envelope == null;
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
