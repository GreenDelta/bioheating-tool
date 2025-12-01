package com.greendelta.bioheating.io.citygml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.Coordinate;
import org.openlca.commons.Res;
import org.openlca.commons.Strings;

import com.greendelta.bioheating.citygml.GmlAddress;
import com.greendelta.bioheating.citygml.GmlBuilding;
import com.greendelta.bioheating.citygml.GmlFunctionType;
import com.greendelta.bioheating.citygml.GmlRoofType;
import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.BuildingType;
import com.greendelta.bioheating.model.Inclusion;
import com.greendelta.bioheating.predict.FeatureValue;

/// Maps the building attributes of the CityGML model to the domain model.
class BuildingProcessor {

	private final List<BuildingShape> shapes;
	private final Map<String, GmlFunctionType> functionTypes;
	private final Map<String, GmlRoofType> roofTypes;

	private BuildingProcessor(List<BuildingShape> shapes) {
		this.shapes = shapes;
		this.functionTypes = GmlFunctionType.getAll();
		this.roofTypes = GmlRoofType.getAll();
	}

	static Res<List<Building>> map(List<BuildingShape> shapes) {
		return shapes == null || shapes.isEmpty()
			? Res.error("No building shapes provided")
			: new BuildingProcessor(shapes).run();
	}

	private Res<List<Building>> run() {
		try {
			var buildings = new ArrayList<Building>(shapes.size());
			for (var shape : shapes) {
				var building = buildingOf(shape);
				buildings.add(building);
			}
			return Res.ok(buildings);
		} catch (Exception e) {
			return Res.error("Failed to map building attributes", e);
		}
	}

	private Building buildingOf(BuildingShape shape) {
		var gml = shape.gml();

		var func = functionTypes.get(gml.function());
		var isHeated = func != null
			? func.isHeated()
			: gml.address() != null;
		var roofType = roofTypes.get(gml.roofType());
		var type = isHeated
			? typeOf(shape)
			: BuildingType.OTHER;
		int storeys = storeysOf(gml, type);

		var building = new Building()
			.name(nameOf(gml))
			.coordinates(coordinatesOf(gml))
			.isHeated(isHeated)
			.type(type)
			.height(gml.height())
			.storeys(storeys)
			.groundArea(shape.groundArea())
			.inclusion(Inclusion.EXCLUDED);

		if (func != null) {
			building.functionCode(func.code())
				.functionLabel(func.label());
		}
		if (roofType != null) {
			building.roofTypeCode(roofType.code())
				.roofTypeLabel(roofType.label());
		}
		mapAddress(gml.address(), building);

		return building;
	}

	private BuildingType typeOf(BuildingShape shape) {
		if (shape.height() > 25)
			return BuildingType.HIGH_RISE;
		if (shape.height() > 12) {
			if (shape.blockVolume() < 2000)
				return BuildingType.MULTI_FAMILY_SMALL;
			return shape.blockVolume() < 5000
				? BuildingType.MULTI_FAMILY_MEDIUM
				: BuildingType.MULTI_FAMILY_LARGE;
		}

		if (shape.groundArea() > 150)
			return BuildingType.MULTI_FAMILY_SMALL;
		if (shape.groundArea() < 30)
			return BuildingType.BUILDING_PART;

		return switch (shape.neighborCount()) {
			case 0 -> BuildingType.SINGLE_FAMILY;
			case 1 -> BuildingType.END_TERRACE;
			case 2 -> BuildingType.MID_TERRACE;
			default -> BuildingType.HOUSE_GROUP;
		};
	}

	private String nameOf(GmlBuilding gml) {
		var address = gml.address();
		if (address == null)
			return gml.id();

		var street = address.street();
		var number = address.number();
		if (Strings.isBlank(street))
			return gml.id();
		return Strings.isBlank(number)
			? street
			: street + " " + number;
	}

	private Coordinate[] coordinatesOf(GmlBuilding gml) {
		var polygon = gml.groundSurface();
		if (polygon == null)
			return null;
		var shell = polygon.getExteriorRing();
		return shell != null
			? shell.getCoordinates()
			: null;
	}

	private void mapAddress(GmlAddress a, Building b) {
		if (a == null)
			return;
		b.country(a.country())
			.locality(a.locality())
			.postalCode(a.postalCode())
			.street(a.street())
			.streetNumber(a.number());
	}

	private static int storeysOf(GmlBuilding gml, BuildingType type) {
		if (gml.storeys() > 0)
			return gml.storeys();
		var hs = FeatureValue.defaultStoreyHeight(type);
		var storeys = (int) Math.round(gml.height() / hs);
		return Math.max(storeys, 1);
	}
}
