package com.greendelta.bioheating.io.citygml;

import org.locationtech.jts.geom.Coordinate;

import com.greendelta.bioheating.citygml.GmlAddress;
import com.greendelta.bioheating.citygml.GmlBuilding;
import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.Inclusion;
import com.greendelta.bioheating.util.Strings;

record BuildingData(GmlBuilding gml, Building building) {

	static BuildingData of(GmlBuilding gml) {
		var cs = coordinatesOf(gml);
		if (cs == null)
			return new BuildingData(gml, null);
		var building = new Building()
			.name(nameOf(gml))
			.coordinates(cs)
			.height(gml.height())
			.inclusion(Inclusion.EXCLUDED);

		mapAddress(gml.address(), building);
		return new BuildingData(gml, building);
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
		return gml == null || building == null;
	}

}
