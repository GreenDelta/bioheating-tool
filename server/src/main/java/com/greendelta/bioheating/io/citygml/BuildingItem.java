package com.greendelta.bioheating.io.citygml;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

import com.greendelta.bioheating.citygml.GmlAddress;
import com.greendelta.bioheating.citygml.GmlBuilding;
import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.Inclusion;
import com.greendelta.bioheating.util.Strings;

record BuildingItem(
	GmlBuilding gml,
	Building building,
	Envelope envelope,
	double groundArea,
	double blockVolume,
	Set<String> neighbors) {

	static BuildingItem of(GmlBuilding gml) {
		var cs = coordinatesOf(gml);
		if (cs == null)
			return new BuildingItem(gml, null, null, 0, 0, null);
		var building = new Building()
			.name(nameOf(gml))
			.coordinates(cs)
			.height(gml.height())
			.inclusion(Inclusion.EXCLUDED);
		mapAddress(gml.address(), building);
		var envelope = gml.groundSurface().getEnvelopeInternal();
		double groundArea = gml.groundSurface().getArea();
		double blockVolume = gml.height() * groundArea;
		return new BuildingItem(
			gml, building, envelope, groundArea, blockVolume, new HashSet<>());
	}

	private static Coordinate[] coordinatesOf(GmlBuilding b) {
		if (b == null)
			return null;
		var polygon = b.groundSurface();
		if (polygon == null)
			return null;
		var shell = polygon.getExteriorRing();
		return shell != null
			? shell.getCoordinates()
			: null;
	}

	private static String nameOf(GmlBuilding b) {
		var address = b.address();
		if (address == null)
			return b.id();

		var street = address.street();
		var number = address.number();
		if (Strings.isNil(street))
			return b.id();
		return Strings.isNil(number)
			? street
			: street + " " + number;
	}

	private static void mapAddress(GmlAddress a, Building b) {
		if (a == null)
			return;
		b.country(a.country())
			.locality(a.locality())
			.postalCode(a.postalCode())
			.street(a.street())
			.streetNumber(a.number());
	}

	boolean isEmpty() {
		return gml == null || building == null || envelope == null;
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
		return obj instanceof BuildingItem other
			&& Objects.equals(this.id(), other.id());
	}
}
