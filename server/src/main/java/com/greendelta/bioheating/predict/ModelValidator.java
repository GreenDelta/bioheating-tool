package com.greendelta.bioheating.predict;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.openlca.commons.Res;

import ml.dmlc.xgboost4j.java.Booster;

public class ModelValidator {

	private final Booster model;
	private final List<CsvItem> items;

	private ModelValidator(Booster model, List<CsvItem> items) {
		this.model = Objects.requireNonNull(model);
		this.items = Objects.requireNonNull(items);
	}

	public static Res<ValidationResult> validate(Booster model, File csvFile) {
		var itemsRes = CsvItem.readFrom(csvFile);
		if (itemsRes.isError()) return itemsRes.wrapError(
			"Failed to read validation data"
		);
		return new ModelValidator(model, itemsRes.value()).run();
	}

	public static Res<ValidationResult> validate(
		Booster model,
		List<CsvItem> items
	) {
		if (items == null || items.isEmpty()) return Res.error(
			"No validation data provided"
		);
		return new ModelValidator(model, items).run();
	}

	private Res<ValidationResult> run() {
		try {
			// Encode features without labels for prediction
			var matrixRes = CsvEncoder.withoutLabels().encode(items);
			if (matrixRes.isError()) return matrixRes.wrapError(
				"Failed to encode validation data"
			);

			// Get predictions
			var predictions = model.predict(matrixRes.value());
			if (predictions.length != items.size()) return Res.error(
				"Prediction count mismatch"
			);

			// Build results
			var results = new ArrayList<ValidationPair>(items.size());
			double sumSquaredError = 0.0;
			double sumActual = 0.0;
			double sumAbsoluteError = 0.0;

			for (int i = 0; i < items.size(); i++) {
				var actual = items.get(i).heatDemand();
				var predicted = (double) predictions[i][0];
				results.add(
					new ValidationPair(items.get(i).buildingId(), actual, predicted)
				);

				var error = actual - predicted;
				sumSquaredError += error * error;
				sumAbsoluteError += Math.abs(error);
				sumActual += actual;
			}

			var n = items.size();
			var metrics = new ValidationMetrics(
				Math.sqrt(sumSquaredError / n), // RMSE
				sumAbsoluteError / n, // MAE
				sumSquaredError / n, // MSE
				calculateR2(results, sumActual / n) // R²
			);

			return Res.ok(new ValidationResult(results, metrics));
		} catch (Exception e) {
			return Res.error("Validation failed", e);
		}
	}

	private double calculateR2(List<ValidationPair> results, double meanActual) {
		double ssTotal = 0.0;
		double ssResidual = 0.0;

		for (var pair : results) {
			var error = pair.actual() - pair.predicted();
			ssResidual += error * error;
			var deviation = pair.actual() - meanActual;
			ssTotal += deviation * deviation;
		}

		return ssTotal == 0.0 ? 0.0 : 1.0 - (ssResidual / ssTotal);
	}

	/// A pair of actual and predicted heat demand values for a single building.
	///
	/// @param buildingId the building identifier
	/// @param actual the actual heat demand in kWh
	/// @param predicted the predicted heat demand in kWh
	public record ValidationPair(
		String buildingId,
		double actual,
		double predicted
	) {
		public double error() {
			return actual - predicted;
		}

		public double absoluteError() {
			return Math.abs(error());
		}

		public double squaredError() {
			return error() * error();
		}
	}

	/// Metrics describing the quality of model predictions.
	///
	/// @param rmse Root Mean Squared Error - lower is better
	/// @param mae Mean Absolute Error - lower is better
	/// @param mse Mean Squared Error - lower is better
	/// @param r2 R-squared (coefficient of determination) - closer to 1.0 is better
	public record ValidationMetrics(
		double rmse,
		double mae,
		double mse,
		double r2
	) {
		@Override
		public String toString() {
			return String.format(
				"RMSE: %.2f, MAE: %.2f, MSE: %.2f, R²: %.4f",
				rmse,
				mae,
				mse,
				r2
			);
		}
	}

	/// The result of model validation.
	///
	/// @param pairs individual prediction results for each building
	/// @param metrics aggregated quality metrics
	public record ValidationResult(
		List<ValidationPair> pairs,
		ValidationMetrics metrics
	) {}
}
