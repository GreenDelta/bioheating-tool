package com.greendelta.bioheating.predict;

import com.greendelta.bioheating.model.BuildingType;

/// Default values that are still used when building data is imported.
///
/// The model itself only uses raw values; the default storey height remains
/// here until the storeys attribute is removed from the domain model.
public class FeatureValue {

	private FeatureValue() {}

	/// Returns the default storey height in meters for the building type.
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
}
