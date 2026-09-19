package com.greendelta.bioheating.predict;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import ml.dmlc.xgboost4j.java.DMatrix;
import org.openlca.commons.Res;

/// Validates the trained models against CSV data and computes common
/// regression statistics for both targets.
public class ModelValidator {

	private final BoostPredictor predictor;
	private final List<CsvItem> items;

	private ModelValidator(BoostPredictor predictor, List<CsvItem> items) {
		this.predictor = Objects.requireNonNull(predictor);
		this.items = Objects.requireNonNull(items);
	}

	/// Validates the models against the data of the given CSV file.
	public static Res<ValidationResult> validate(
		BoostPredictor predictor,
		File csvFile
	) {
		var items = CsvItem.readFrom(csvFile);
		if (items.isError()) return items.wrapError(
			"Failed to read validation data"
		);
		return validate(predictor, items.value());
	}

	/// Validates the models against the given data rows.
	public static Res<ValidationResult> validate(
		BoostPredictor predictor,
		List<CsvItem> items
	) {
		if (predictor == null || items == null || items.isEmpty()) {
			return Res.error("No model or validation data provided");
		}
		return new ModelValidator(predictor, items).run();
	}

	private Res<ValidationResult> run() {
		var features = CsvEncoder.encode(items);
		if (features.isError()) return features.wrapError(
			"Failed to encode validation data"
		);
		try {
			var heatDemand = validate(Target.HEAT_DEMAND, features.value());
			var peakLoad = validate(Target.PEAK_LOAD, features.value());
			return Res.ok(new ValidationResult(heatDemand, peakLoad));
		} catch (Exception e) {
			return Res.error("Validation failed", e);
		}
	}

	private TargetResult validate(Target target, DMatrix features)
		throws Exception {
		var predictions = predictor.of(target).predict(features);
		var values = new ArrayList<TargetValue>(items.size());
		for (var i = 0; i < items.size(); i++) {
			values.add(
				new TargetValue(target.labelOf(items.get(i)), predictions[i][0])
			);
		}
		return new TargetResult(values, metricsOf(values));
	}

	private static ValidationMetrics metricsOf(List<TargetValue> values) {
		double sumAbsolute = 0;
		double sumSquared = 0;
		double sumBias = 0;
		double sumExpected = 0;
		for (var value : values) {
			var error = value.expected() - value.predicted();
			sumAbsolute += Math.abs(error);
			sumSquared += error * error;
			sumBias += error;
			sumExpected += value.expected();
		}

		var n = values.size();
		var mean = sumExpected / n;
		double ssResidual = 0;
		double ssTotal = 0;
		for (var value : values) {
			ssResidual += Math.pow(value.expected() - value.predicted(), 2);
			ssTotal += Math.pow(value.expected() - mean, 2);
		}

		return new ValidationMetrics(
			sumAbsolute / n, // MAE
			Math.sqrt(sumSquared / n), // RMSE
			sumBias / n, // MBE
			ssTotal == 0 ? 0 : 1 - ssResidual / ssTotal // R2
		);
	}

	/// The expected and the predicted value of one target for a single row.
	///
	/// @param expected the expected value from the CSV data
	/// @param predicted the value predicted by the model
	public record TargetValue(double expected, double predicted) {}

	/// The validation values and metrics of one target.
	///
	/// @param values the expected and predicted values of each data row
	/// @param metrics the aggregated error statistics
	public record TargetResult(
		List<TargetValue> values,
		ValidationMetrics metrics
	) {}

	/// Common regression statistics. Lower MAE, RMSE and MBE are better and an
	/// R2 closer to 1.0 is better.
	///
	/// @param mae mean absolute error
	/// @param rmse root mean squared error
	/// @param mbe mean bias error; positive means the model over-predicts
	/// @param r2 coefficient of determination
	public record ValidationMetrics(
		double mae,
		double rmse,
		double mbe,
		double r2
	) {
		@Override
		public String toString() {
			return String.format(
				"MAE: %.3f, RMSE: %.3f, MBE: %.3f, R2: %.4f",
				mae,
				rmse,
				mbe,
				r2
			);
		}
	}

	/// The validation result of both targets.
	///
	/// @param heatDemand the result for the annual heat demand in kWh
	/// @param peakLoad the result for the peak heating load in kW
	public record ValidationResult(
		TargetResult heatDemand,
		TargetResult peakLoad
	) {}
}
