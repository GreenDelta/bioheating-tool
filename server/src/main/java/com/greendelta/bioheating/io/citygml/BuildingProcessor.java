package com.greendelta.bioheating.io.citygml;

import com.greendelta.bioheating.citygml.GmlAddress;
import com.greendelta.bioheating.citygml.GmlBuilding;
import com.greendelta.bioheating.citygml.GmlFunctionType;
import com.greendelta.bioheating.citygml.GmlRoofType;
import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.BuildingType;
import com.greendelta.bioheating.predict.FeatureValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

import org.locationtech.jts.geom.Coordinate;
import org.openlca.commons.Res;
import org.openlca.commons.Strings;

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

			// first pass: initialize the buildings
			var buildings = new ArrayList<Building>(shapes.size());
			var shapeMap = new HashMap<String, BuildingShape>(shapes.size());
			var buildingMap = new HashMap<String, Building>(shapes.size());
			for (var shape : shapes) {
				var building = buildingOf(shape);
				buildings.add(building);
				shapeMap.put(shape.id(), shape);
				buildingMap.put(shape.id(), building);
			}

			// count the number of heated neighbors of a building
			ToIntFunction<String> neighbors = (id) -> {
				var shape = shapeMap.get(id);
				if (shape == null
					|| shape.neighbors() == null
					|| shape.neighbors().isEmpty()) {
					return 0;
				}
				int count = 0;
				for (var ni : shape.neighbors()) {
					var other = buildingMap.get(ni);
					if (other != null && other.isHeated()) {
						count++;
					}
				}
				return count;
			};

			// second pass: count the heated neighbors and
			for (var b : buildings) {
				if (!b.isHeated()) continue;
				var shape = shapeMap.get(b.cityId());
				if (shape == null) continue;
				var type = typeOf(shape, neighbors);
				int storeys = storeysOf(shape.gml(), type);
				b.type(type).storeys(storeys);
			}

			return Res.ok(buildings);
		} catch (Exception e) {
			return Res.error("Failed to map building attributes", e);
		}
	}

	private Building buildingOf(BuildingShape shape) {
		var gml = shape.gml();

		var func = functionTypes.get(gml.function());
		var isHeated = gml.address() != null || (func != null && func.isHeated());

		var b = new Building()
			.name(nameOf(gml))
			.cityId(gml.id())
			.coordinates(coordinatesOf(gml))
			.isHeated(isHeated)
			.height(gml.height())
			.groundArea(shape.groundArea())
			.isIncluded(false)
			.type(BuildingType.OTHER); // updated later

		if (func != null) {
			b.functionCode(func.code());
			b.functionLabel(func.label());
		}

		var roofType = roofTypes.get(gml.roofType());
		if (roofType != null) {
			b.roofTypeCode(roofType.code());
			b.roofTypeLabel(roofType.label());
		}
		mapAddress(gml.address(), b);
		return b;
	}

	private BuildingType typeOf(
		BuildingShape shape,
		ToIntFunction<String> neighbors
	) {
		if (shape.height() > 25) {
			return BuildingType.HIGH_RISE;
		}
		if (shape.height() > 12) {
			if (shape.blockVolume() < 2000) {
				return BuildingType.MULTI_FAMILY_SMALL;
			}
			return shape.blockVolume() < 5000
				? BuildingType.MULTI_FAMILY_MEDIUM
				: BuildingType.MULTI_FAMILY_LARGE;
		}

		if (shape.groundArea() > 150) {
			return BuildingType.MULTI_FAMILY_SMALL;
		}
		if (shape.groundArea() < 30) {
			return BuildingType.BUILDING_PART;
		}

		return switch (neighbors.applyAsInt(shape.id())) {
			case 0 -> BuildingType.SINGLE_FAMILY;
			case 1 -> BuildingType.END_TERRACE;
			case 2 -> BuildingType.MID_TERRACE;
			default -> BuildingType.HOUSE_GROUP;
		};
	}

	private String nameOf(GmlBuilding gml) {
		var address = gml.address();
		if (address == null) return gml.id();

		var street = address.street();
		var number = address.number();
		if (Strings.isBlank(street)) return gml.id();
		return Strings.isBlank(number) ? street : street + " " + number;
	}

	private Coordinate[] coordinatesOf(GmlBuilding gml) {
		var polygon = gml.groundSurface();
		if (polygon == null) return null;
		var shell = polygon.getExteriorRing();
		return shell != null ? shell.getCoordinates() : null;
	}

	private void mapAddress(GmlAddress a, Building b) {
		if (a == null) return;
		b
			.country(a.country())
			.locality(a.locality())
			.postalCode(a.postalCode())
			.street(a.street())
			.streetNumber(a.number());
	}

	private static int storeysOf(GmlBuilding gml, BuildingType type) {
		if (gml.storeys() > 0) return gml.storeys();
		var hs = FeatureValue.defaultStoreyHeight(type);
		var storeys = (int) Math.round(gml.height() / hs);
		return Math.max(storeys, 1);
	}
}
