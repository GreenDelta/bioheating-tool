package com.greendelta.bioheating.predict;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.ClimateRegion;
import org.openlca.commons.Res;

public record BuildingEstimator(BoostPredictor predictor) {

	public static Res<BuildingEstimator> getDefault() {
		var predictor = BoostPredictor.getDefault();
		return predictor.isError()
			? Res.error("failed to load heat demand predictor: " + predictor.error())
			: Res.ok(new BuildingEstimator(predictor.value()));
	}

	public Res<Result> estimate(ClimateRegion region, Building building) {
		if (region == null || building == null) {
			return Res.error("climate region or building missing");
		}
		var heatDemand = predictor.predict(region, building);
		if (heatDemand.isError()) {
			return Res.error("failed to predict heat demand: " + heatDemand.error());
		}
		var demand = heatDemand.value();
		return Res.ok(new Result(demand, peakLoadOf(demand)));
	}

	public static double peakLoadOf(double heatDemand) {
		return 0.000451213244244867 * heatDemand + 3.66786593448211;
	}

	public record Result(double heatDemand, double peakLoad) {}
}
