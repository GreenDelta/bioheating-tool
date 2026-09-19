package com.greendelta.bioheating.predict;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.ClimateRegion;
import org.openlca.commons.Res;

/// Estimates the heat demand and the peak load of a single building.
public record BuildingEstimator(BoostPredictor predictor) {

	public static Res<BuildingEstimator> getDefault() {
		var predictor = BoostPredictor.getDefault();
		return predictor.isError()
			? Res.error(
				"failed to load heat demand predictor: " + predictor.error()
			)
			: Res.ok(new BuildingEstimator(predictor.value()));
	}

	/// Predicts both targets for the given building.
	public Res<BoostPredictor.Prediction> estimate(
		ClimateRegion region,
		Building building
	) {
		if (region == null || building == null) {
			return Res.error("climate region or building missing");
		}
		var prediction = predictor.predict(region, building);
		return prediction.isError()
			? prediction.wrapError(
				"failed to predict heat demand and peak load"
			)
			: prediction;
	}
}
