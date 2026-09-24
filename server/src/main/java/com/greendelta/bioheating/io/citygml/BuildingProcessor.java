package com.greendelta.bioheating.io.citygml;

import com.greendelta.bioheating.citygml.GmlAddress;
import com.greendelta.bioheating.citygml.GmlBuilding;
import com.greendelta.bioheating.citygml.GmlFunctionType;
import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.BuildingType;
import com.greendelta.bioheating.model.RoofType;
import com.greendelta.bioheating.model.WarmWater;

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

	private BuildingProcessor(List<BuildingShape> shapes) {
		this.shapes = shapes;
		this.functionTypes = GmlFunctionType.getAll();
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

			// second pass: assign the building type from the building parameters
			// and the heated neighbors, and look up the warm water fraction for
			// the type and the construction age
			for (var b : buildings) {
				var shape = shapeMap.get(b.cityId());
				if (shape == null)
					continue;
				var type = BuildingType.estimateFrom(
					shape.height(),
					shape.groundArea(),
					() -> neighbors.applyAsInt(shape.id()));
				b.type(type);
				b.warmWaterFraction(WarmWater.of(type, b.constructionAge()));
			}

			return Res.ok(buildings);
		} catch (Exception e) {
			return Res.error("Failed to map building attributes", e);
		}
	}

	private Building buildingOf(BuildingShape shape) {
		var gml = shape.gml();
		var func = functionTypes.get(gml.function());

		var b = new Building()
			.name(nameOf(gml))
			.cityId(gml.id())
			.coordinates(coordinatesOf(gml))
			.isHeated(isHeated(shape, func))
			.height(gml.height())
			.groundArea(shape.groundArea())
			.isIncluded(false)
			.type(BuildingType.MULTI_GENERATION); // updated later

		if (func != null) {
			b.functionCode(func.code());
			b.functionLabel(func.label());
		}

		b.roofType(roofTypeOf(gml));
		mapAddress(gml.address(), b);
		return b;
	}

	private boolean isHeated(BuildingShape shape, GmlFunctionType func) {
		if (shape.groundArea() < 20 || shape.height() < 2)
			return false;
		if (func != null && !func.isHeated())
			return false;
		return shape.gml().address() != null || func != null;
	}


	/// The model only distinguishes flat and pitched roofs; only "1000"
	/// (Flachdach) is a flat roof in the CityGML data.
	private static RoofType roofTypeOf(GmlBuilding gml) {
		return "1000".equals(gml.roofType())
			? RoofType.FLAT
			: RoofType.PITCHED;
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
		if (a == null)
			return;
		b.country(a.country())
			.locality(a.locality())
			.postalCode(a.postalCode())
			.street(a.street())
			.streetNumber(a.number());
	}

}
