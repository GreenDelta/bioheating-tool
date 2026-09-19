package com.greendelta.bioheating.predict;

import java.util.List;
import java.util.Objects;
import ml.dmlc.xgboost4j.java.DMatrix;
import org.openlca.commons.Res;

/// Encodes CSV data rows into an XGBoost matrix.
class CsvEncoder {

	private CsvEncoder() {}

	/// Encodes the features of the given rows without labels.
	static Res<DMatrix> encode(List<CsvItem> items) {
		return run(items, null);
	}

	/// Encodes the features of the given rows and adds the labels of the given
	/// target for training.
	static Res<DMatrix> encodeWithLabels(List<CsvItem> items, Target target) {
		return run(items, Objects.requireNonNull(target));
	}

	private static Res<DMatrix> run(List<CsvItem> items, Target target) {
		if (items == null || items.isEmpty()) return Res.error(
			"CSV item data missing"
		);
		try {
			var data = new float[Features.COUNT * items.size()];
			for (var i = 0; i < items.size(); i++) {
				Features.of(items.get(i), data, i * Features.COUNT);
			}
			var matrix = new DMatrix(
				data,
				items.size(),
				Features.COUNT,
				Float.NaN
			);
			if (target != null) {
				var labels = new float[items.size()];
				for (var i = 0; i < items.size(); i++) {
					labels[i] = (float) target.labelOf(items.get(i));
				}
				matrix.setLabel(labels);
			}
			return Res.ok(matrix);
		} catch (Exception e) {
			return Res.error("Failed to encode CSV data", e);
		}
	}
}
