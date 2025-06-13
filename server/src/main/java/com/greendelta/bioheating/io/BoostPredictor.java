package com.greendelta.bioheating.io;

import java.util.List;

import com.greendelta.bioheating.model.Building;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;

public record BoostPredictor(Booster booster) {

	public static BoostPredictor getDefault() {
		var stream = BoostPredictor.class.getResourceAsStream("model.json");
		if (stream == null)
			throw new RuntimeException("default model not found");
		try (stream) {
			var booster = XGBoost.loadModel(stream);
			return new BoostPredictor(booster);
		} catch (Exception e) {
			throw new RuntimeException("failed to load default model", e);
		}
	}

	public float predict(Building b) {
		return b != null
			? predictOne(BoostEncoder.encode(b))
			: 0;
	}

	public float[] predictAll(List<Building> bs) {
		if (bs == null || bs.isEmpty())
			return new float[0];
		var data = BoostEncoder.encodeBuildingData(bs);
		return predictAll(data);
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

	private float[] predictAll(DMatrix matrix) {
		try {
			var predictions = booster.predict(matrix);
			var ret = new float[predictions.length];
			for (int i = 0; i < predictions.length; i++) {
				ret[i] = predictions[i][0];
			}
			return ret;
		} catch (Exception e) {
			throw new RuntimeException("failed to predict values", e);
		}
	}

	public record PredictedValue<T>(T entity, double value) {
	}
}
