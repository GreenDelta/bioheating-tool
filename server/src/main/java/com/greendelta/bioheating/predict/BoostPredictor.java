package com.greendelta.bioheating.predict;

import java.util.List;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.ClimateRegion;
import com.greendelta.bioheating.util.Res;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;

public record BoostPredictor(Booster booster) {

	public static Res<BoostPredictor> getDefault() {
		var stream = BoostPredictor.class.getResourceAsStream("model.bin");
		if (stream == null)
			return Res.error("Default model not found");
		try (stream) {
			var booster = XGBoost.loadModel(stream);
			var predictor = new BoostPredictor(booster);
			return Res.of(predictor);
		} catch (Exception e) {
			return Res.error("Failed to load default model", e);
		}
	}

	public Res<Float> predict(ClimateRegion region, Building b) {		
		if (b == null)
			return Res.error("No building data provided");
		var res = predictAll(region, List.of(b));
		if (res.hasError())
			return res.castError();
		var xs = res.value();
		return xs.length == 0
			? Res.error("Invalid value predicted")
			: Res.of(xs[0]);
	}

	public Res<float[]> predictAll(ClimateRegion region, List<Building> bs) {
		var encoded = BuildingEncoder.encode(region, bs);
		return encoded.hasError()
			? encoded.wrapError("Failed to encode building data")
			: predict(encoded.value());
	}

	private float predictOne(float[] data) {
		try {
			var matrix = new DMatrix(data, 1, data.length, Float.NaN);
			var predictions = booster.predict(matrix);
			return predictions[0][0];
		} catch (Exception e) {
			throw new RuntimeException("failed to predict value", e);
		}
	}

	private Res<float[]> predict(DMatrix matrix) {
		try {
			var predictions = booster.predict(matrix);
			var ret = new float[predictions.length];
			for (int i = 0; i < predictions.length; i++) {
				ret[i] = predictions[i][0];
			}
			return Res.of(ret);
		} catch (Exception e) {
			return Res.error("Prediction failed", e);
		}
	}
}
