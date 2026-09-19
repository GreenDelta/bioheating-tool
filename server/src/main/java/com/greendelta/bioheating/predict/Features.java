package com.greendelta.bioheating.predict;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.BuildingType;
import com.greendelta.bioheating.model.ConstructionAge;
import com.greendelta.bioheating.model.RoofType;

/// Builds the feature vector of the model. Both training and prediction use
/// this class, so the feature order can never diverge.
///
/// The model expects these six raw values in this order:
/// ground area [m2], height [m], weather station [code],
/// construction year [code], roof type [1|0], building type [code].
class Features {

	/// The number of features that the model consumes.
	static final int COUNT = 6;

	private Features() {}

	/// Encodes the features of a CSV data row.
	static void of(CsvItem item, float[] data, int offset) {
		data[offset] = (float) item.groundArea();
		data[offset + 1] = (float) item.height();
		data[offset + 2] = item.weatherStation();
		data[offset + 3] = item.constructionYear();
		data[offset + 4] = item.roofType();
		data[offset + 5] = item.buildingType();
	}

	/// Encodes the features of a building for prediction. The weather station
	/// is the number of the climate region of the project of the building.
	static void of(int weatherStation, Building b, float[] data, int offset) {
		data[offset] = (float) b.groundArea();
		data[offset + 1] = (float) b.height();
		data[offset + 2] = weatherStation;
		data[offset + 3] = constructionYearOf(b);
		data[offset + 4] = roofTypeOf(b);
		data[offset + 5] = buildingTypeOf(b);
	}

	/// The domain model still has an unknown construction age (0); the model
	/// defaults to 1979-1995 (4) for such buildings.
	private static int constructionYearOf(Building b) {
		var age = b.constructionAge();
		if (age == null || age == ConstructionAge.UNKNOWN) return 4;
		return age.code();
	}

	/// The roof type code: 1 for a flat roof, 0 for a pitched roof.
	private static int roofTypeOf(Building b) {
		var roofType = b.roofType();
		return roofType == null ? RoofType.PITCHED.code() : roofType.code();
	}

	/// The model does not know the fallback type `OTHER` (0), so buildings
	/// without a valid type cannot be predicted.
	private static int buildingTypeOf(Building b) {
		var type = b.type();
		if (type == null || type == BuildingType.OTHER) {
			throw new IllegalArgumentException(
				"cannot predict a building without a valid building type"
			);
		}
		return type.code();
	}
}
