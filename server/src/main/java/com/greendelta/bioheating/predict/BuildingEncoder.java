package com.greendelta.bioheating.predict;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.ClimateRegion;
import java.util.List;
import ml.dmlc.xgboost4j.java.DMatrix;
import org.openlca.commons.Res;

/// Encodes buildings into an XGBoost matrix for prediction.
class BuildingEncoder {

	private BuildingEncoder() {}

	static Res<DMatrix> encode(ClimateRegion region, List<Building> buildings) {
		if (region == null || buildings == null || buildings.isEmpty()) {
			return Res.error("Climate region or building data missing");
		}
		try {
			var data = new float[Features.COUNT * buildings.size()];
			for (var i = 0; i < buildings.size(); i++) {
				Features.of(
					region.number(),
					buildings.get(i),
					data,
					i * Features.COUNT
				);
			}
			var matrix = new DMatrix(
				data,
				buildings.size(),
				Features.COUNT,
				Float.NaN
			);
			return Res.ok(matrix);
		} catch (Exception e) {
			return Res.error("Failed to encode building data", e);
		}
	}
}
