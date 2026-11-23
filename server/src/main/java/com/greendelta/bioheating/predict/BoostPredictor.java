package com.greendelta.bioheating.predict;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.ClimateRegion;
import com.greendelta.bioheating.model.WarmWater;

import java.io.File;
import java.io.FileInputStream;
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
/// @param config the configuration with the corrections of the raw outputs
public record BoostPredictor(
	Booster heatDemand,
	Booster peakLoad,
	ModelConfig config
) {

	/// Creates a predictor without corrections of the model outputs.
	public BoostPredictor(Booster heatDemand, Booster peakLoad) {
		this(heatDemand, peakLoad, ModelConfig.NONE);
	}

	/// Loads the two trained models and the model configuration from the
	/// given model directory, see `ModelConfig`.
	public static Res<BoostPredictor> load(File dir) {
		if (dir == null || !dir.isDirectory()) {
			return Res.error("Model directory not found: "
				+ (dir != null ? dir.getAbsolutePath() : "null"));
		}
		var heatDemand = load(dir, Target.HEAT_DEMAND);
		if (heatDemand.isError())
			return heatDemand.castError();

		var peakLoad = load(dir, Target.PEAK_LOAD);
		if (peakLoad.isError())
			return peakLoad.castError();

		var config = ModelConfig.read(dir);
		if (config.isError())
			return config.castError();

		return Res.ok(
			new BoostPredictor(heatDemand.value(), peakLoad.value(), config.value())
		);
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
	/// corrected with the configured correction of the target. As the models
	/// predict space heating values, the warm water demand of the building is
	/// added afterwards: its full share to the heat demand and its constant base
	/// load to the peak load.
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
			var demandCorrection = config.correctionOf(Target.HEAT_DEMAND);
			var peakCorrection = config.correctionOf(Target.PEAK_LOAD);
			var predictions = new ArrayList<Prediction>(demands.length);
			for (var i = 0; i < demands.length; i++) {
				var building = buildings.get(i);
				var spaceDemand = demandCorrection.apply(demands[i]);
				var spacePeakLoad = peakCorrection.apply(peaks[i]);
				predictions.add(
					new Prediction(
						WarmWater.addToDemand(building, spaceDemand),
						WarmWater.addToPeakLoad(building, spacePeakLoad, spaceDemand)
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

	private static Res<Booster> load(File dir, Target target) {
		var file = new File(dir, target.modelFile());
		if (!file.isFile()) return Res.error(
			"Model file not found: " + file.getAbsolutePath()
		);
		try (InputStream in = new FileInputStream(file)) {
			return Res.ok(XGBoost.loadModel(in));
		} catch (Exception e) {
			return Res.error("Failed to load model: " + file.getAbsolutePath(), e);
		}
	}

	/// The predicted values of both targets for one building. Both values include
	/// the warm water demand of the building.
	///
	/// @param heatDemand the total annual heat demand in kWh
	/// @param peakLoad the total peak heating load in kW
	public record Prediction(double heatDemand, double peakLoad) {
	}
}
