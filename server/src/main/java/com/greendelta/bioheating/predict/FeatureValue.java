package com.greendelta.bioheating.predict;

import com.greendelta.bioheating.model.BuildingType;
import com.greendelta.bioheating.model.Project;

public class FeatureValue {
	private FeatureValue() {
	}

	public static float ofClimateRegion(Project project) {
		return project != null && project.climateRegion() != null
			? ofClimateRegion(project.climateRegion().number())
			: 0.91f;
	}

	public static float ofClimateRegion(int code) {
		return switch (code) {
			case 1 -> 0.85f;
			case 2 -> 1.08f;
			case 3 -> 0.83f;
			case 4 -> 0.84f;
			case 5 -> 1.01f;
			case 6 -> 0.8f;
			case 7 -> 1.06f;
			case 8 -> 0.84f;
			case 9 -> 0.89f;
			case 10 -> 1.06f;
			case 11 -> 1.16f;
			case 12 -> 0.91f;
			case 13 -> 1.15f;
			case 14 -> 1.19f;
			case 15 -> 0.89f;
			default -> 0.91f;
		};
	}

	public static float ofBuildingType(BuildingType type) {
		if (type == null)
			return 0.8f;
		return switch (type) {
			case HIGH_RISE -> 0.65f;
			case MULTI_FAMILY_SMALL -> 0.8f;
			case MULTI_FAMILY_MEDIUM -> 0.75f;
			case MULTI_FAMILY_LARGE -> 0.7f;
			case BUILDING_PART -> 0.8f;
			case SINGLE_FAMILY -> 1.0f;
			case END_TERRACE -> 0.9f;
			case MID_TERRACE -> 0.8f;
			case HOUSE_GROUP -> 0.88f;
			case OTHER -> 0.8f;
		};
	}

	/// Returns the specific heat demand in kWh/m2/a for the given construction
	/// year. The provided year can be a range, like `1979-1995`.
	public static float ofConstructionYear(String range) {
		if (range == null || range.isBlank())
			return 130;
		var parts = range.split("-");

		int start = atoi(parts[0], 0);
		if (start == 0)
			return 130;
		if (parts.length < 2)
			return ofConstructionYear(start);

		int end = atoi(parts[1], 0);
		if (end == 0)
			return ofConstructionYear(start);

		float a = ofConstructionYear(start);
		float b = ofConstructionYear(end);
		return a == b ? a : (a + b) / 2;
	}

	private static int atoi(String s, int otherwise) {
		if (s == null || s.isBlank())
			return otherwise;
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
			return otherwise;
		}
	}

	/// Returns the specific heat demand in kWh/m2/a for the given year of
	/// construction of a building.
	public static float ofConstructionYear(int year) {
		if (year < 1900)
			return 130;
		if (year < 1919)
			return 180;
		if (year < 1948)
			return 190;
		if (year < 1978)
			return 210;
		if (year < 1995)
			return 150;
		if (year < 2009)
			return 80;
		return 50;
	}

	/// Returns the feature factor for the given CityGML code of a roof type.
	public static float ofRoofType(String code) {
		if (code == null)
			return 0.85f;
		return switch (code) {
			case "1000" -> 1f;
			case "2100" -> 0.95f;
			case "2200" -> 0.9f;
			case "3100" -> 0.85f;
			case "3200" -> 0.8f;
			case "3300" -> 0.85f;
			case "3400" -> 0.9f;
			case "3500" -> 0.75f;
			case "3600" -> 0.7f;
			case "3700" -> 0.85f;
			case "3800" -> 0.9f;
			case "3900" -> 0.85f;
			case "4000" -> 0.7f;
			case "5000" -> 0.85f;
			case "9999" -> 0.85f;
			default -> 0.85f;
		};
	}

}
