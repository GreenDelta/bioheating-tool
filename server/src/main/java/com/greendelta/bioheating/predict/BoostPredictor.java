package com.greendelta.bioheating.predict;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.ClimateRegion;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.XGBoost;
import org.openlca.commons.Res;

/// Loads the two trained models and predicts the heat demand and the peak
/// load of buildings.
///
/// @param heatDemand the model for the annual heat demand in kWh
/// @param peakLoad the model for the peak heating load in kW
public record BoostPredictor(Booster heatDemand, Booster peakLoad) {

	/// Loads the models that are packaged with the application.
	public static Res<BoostPredictor> getDefault() {
		var heatDemand = load(Target.HEAT_DEMAND);
		if (heatDemand.isError()) return heatDemand.castError();
		var peakLoad = load(Target.PEAK_LOAD);
		if (peakLoad.isError()) return peakLoad.castError();
		return Res.ok(new BoostPredictor(heatDemand.value(), peakLoad.value()));
	}

	/// The booster of the given target.
	public Booster of(Target target) {
		return switch (target) {
			case HEAT_DEMAND -> heatDemand;
			case PEAK_LOAD -> peakLoad;
		};
	}

	/// Predicts both targets for a single building.
	public Res<Prediction> predict(ClimateRegion region, Building b) {
		if (b == null)
			return Res.error("No building data provided");
		var res = predictAll(region, List.of(b));
		if (res.isError())
			return res.castError();
		return Res.ok(res.value().getFirst());
	}

	/// Predicts both targets for the given buildings. The raw model outputs are
	/// corrected with the configured correction of the target.
	public Res<List<Prediction>> predictAll(
		ClimateRegion region,
		List<Building> buildings
	) {
		var encoded = BuildingEncoder.encode(region, buildings);
		if (encoded.isError()) return encoded.wrapError(
			"Failed to encode building data"
		);
		try {
			var matrix = encoded.value();
			var demands = firstColumn(heatDemand.predict(matrix));
			var peaks = firstColumn(peakLoad.predict(matrix));
			var demandCorrection = ModelCorrection.of(Target.HEAT_DEMAND);
			var peakCorrection = ModelCorrection.of(Target.PEAK_LOAD);
			var predictions = new ArrayList<Prediction>(demands.length);
			for (var i = 0; i < demands.length; i++) {
				predictions.add(
					new Prediction(
						demandCorrection.apply(demands[i]),
						peakCorrection.apply(peaks[i])
					)
				);
			}
			return Res.ok(predictions);
		} catch (Exception e) {
			return Res.error("Prediction failed", e);
		}
	}

	private static float[] firstColumn(float[][] predictions) {
		var values = new float[predictions.length];
		for (var i = 0; i < predictions.length; i++) {
			values[i] = predictions[i][0];
		}
		return values;
	}

	private static Res<Booster> load(Target target) {
		var stream = BoostPredictor.class.getResourceAsStream(
			target.modelFile()
		);
		if (stream == null) return Res.error(
			"Model file not found: " + target.modelFile()
		);
		try (InputStream in = stream) {
			return Res.ok(XGBoost.loadModel(in));
		} catch (Exception e) {
			return Res.error("Failed to load model: " + target.modelFile(), e);
		}
	}

	/// The predicted values of both targets for one building.
	///
	/// @param heatDemand the annual heat demand in kWh
	/// @param peakLoad the peak heating load in kW
	public record Prediction(double heatDemand, double peakLoad) {}
}
