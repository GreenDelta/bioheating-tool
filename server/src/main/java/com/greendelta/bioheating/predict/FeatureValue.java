package com.greendelta.bioheating.predict;

import com.greendelta.bioheating.model.BuildingType;
import com.greendelta.bioheating.model.ConstructionAge;
import com.greendelta.bioheating.model.Project;

public class FeatureValue {

	private FeatureValue() {}

	public static float climateRegionFactor(Project project) {
		return project != null && project.climateRegion() != null
			? climateRegionFactor(project.climateRegion().number())
			: 0.91f;
	}

	public static float climateRegionFactor(int code) {
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

	public static float typeFactor(BuildingType type) {
		if (type == null) return 0.8f;
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

	/// Returns the default storey height in meters for the given building type.
	public static float defaultStoreyHeight(BuildingType type) {
		if (type == null) return 2.85f;
		return switch (type) {
			case HIGH_RISE -> 3.3f;
			case MULTI_FAMILY_SMALL -> 3.0f;
			case MULTI_FAMILY_MEDIUM -> 2.9f;
			case MULTI_FAMILY_LARGE -> 3.1f;
			case BUILDING_PART -> 3.0f;
			case SINGLE_FAMILY -> 2.8f;
			case END_TERRACE -> 2.75f;
			case MID_TERRACE -> 2.7f;
			case HOUSE_GROUP -> 2.75f;
			case OTHER -> 2.85f;
		};
	}

	/// Returns the heated area factor for the given building type.
	public static float heatedAreaFactor(BuildingType type) {
		if (type == null) return 0.8f;
		return switch (type) {
			case HIGH_RISE -> 0.75f;
			case MULTI_FAMILY_SMALL -> 0.8f;
			case MULTI_FAMILY_MEDIUM -> 0.78f;
			case MULTI_FAMILY_LARGE -> 0.75f;
			case BUILDING_PART -> 0.8f;
			case SINGLE_FAMILY -> 0.8f;
			case END_TERRACE -> 0.85f;
			case MID_TERRACE -> 0.87f;
			case HOUSE_GROUP -> 0.83f;
			case OTHER -> 0.8f;
		};
	}

	/// Returns the average annual heat demand of a building of the given age in
	/// kWh/m2/year.
	public static float averageHeatDemand(ConstructionAge age) {
		if (age == null) return 130f;
		return switch (age) {
			case UNKNOWN -> 130f;
			case AGE_1900_1919 -> 180f;
			case AGE_1919_1948 -> 190f;
			case AGE_1949_1978 -> 210f;
			case AGE_1979_1995 -> 150f;
			case AGE_1995_2009 -> 80f;
			case AGE_2010_2030 -> 50f;
		};
	}

	/// Returns the feature factor for the given CityGML code of a roof type.
	public static float roofTypeFacor(String code) {
		if (code == null) return 0.85f;
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
