package com.greendelta.bioheating.io.citygml;

import java.util.List;
import java.util.Map;

import com.greendelta.bioheating.citygml.GmlFunctionType;
import com.greendelta.bioheating.citygml.GmlRoofType;
import com.greendelta.bioheating.model.BuildingType;

class BuildingTypes {

	private final List<BuildingShape> shapes;
	private final Map<String, GmlFunctionType> functionTypes;
	private final Map<String, GmlRoofType> roofTypes;

	private BuildingTypes(List<BuildingShape> shapes) {
		this.shapes = shapes;
		this.functionTypes = GmlFunctionType.getAll();
		this.roofTypes = GmlRoofType.getAll();
	}

	static void map(List<BuildingShape> items) {
		if (items == null || items.isEmpty())
			return;
		new BuildingTypes(items).runMapping();
	}

	private void runMapping() {
		for (var shape : shapes) {
			mapRoofType(item);
			mapFunctionType(item);
			// the heated-flag needs to be set now
			item.building()
				.type(buildingTypeOf(item));
		}
	}

	private void mapRoofType(BuildingShape item) {
		var type = roofTypes.get(item.gml().roofType());
		if (type != null) {
			item.building()
				.roofTypeCode(type.code())
				.roofTypeLabel(type.label());
		}
	}

	private void mapFunctionType(BuildingShape item) {
		var type = functionTypes.get(item.gml().function());
		if (type != null) {
			item.building()
				.functionCode(type.code())
				.functionLabel(type.label())
				.isHeated(type.isHeated());
		} else {
			// if we cannot determine the function of the building,
			// we set it to `heated`, if it has an address
			item.building()
				.isHeated(item.gml().address() != null);
		}
	}

	private static BuildingType buildingTypeOf(BuildingShape item) {
		if (!item.building().isHeated())
			return BuildingType.OTHER;
		if (item.height() > 25)
			return BuildingType.HIGH_RISE;
		if (item.height() > 12) {
			if (item.blockVolume() < 2000)
				return BuildingType.MULTI_FAMILY_SMALL;
			return item.blockVolume() < 5000
				? BuildingType.MULTI_FAMILY_MEDIUM
				: BuildingType.MULTI_FAMILY_LARGE;
		}

		if (item.building().groundArea() > 150)
			return BuildingType.MULTI_FAMILY_SMALL;
		if (item.building().groundArea() < 30)
			return BuildingType.BUILDING_PART;

		return switch (item.neighborCount()) {
			case 0 -> BuildingType.SINGLE_FAMILY;
			case 1 -> BuildingType.END_TERRACE;
			case 2 -> BuildingType.MID_TERRACE;
			default -> BuildingType.HOUSE_GROUP;
		};
	}
}
