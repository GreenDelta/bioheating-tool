package com.greendelta.bioheating.predict;

import java.util.List;

import org.openlca.commons.Res;

import com.greendelta.bioheating.model.BuildingType;
import com.greendelta.bioheating.model.ConstructionAge;

import ml.dmlc.xgboost4j.java.DMatrix;

class CsvEncoder {

	private static final int PARAMS = 7;

	private final boolean includeLabels;

	private CsvEncoder(boolean includeLabels) {
		this.includeLabels = includeLabels;
	}

	/// Creates an encoder that includes heat demand labels in the DMatrix.
	/// Use this for training data where the actual heat demand values are known
	/// and need to be provided to the model for training.
	static CsvEncoder withLabels() {
		return new CsvEncoder(true);
	}

	/// Creates an encoder that does not include labels in the DMatrix.
	/// Use this for prediction/validation data where you want to predict
	/// heat demand values using a trained model.
	static CsvEncoder withoutLabels() {
		return new CsvEncoder(false);
	}

	Res<DMatrix> encode(List<CsvItem> items) {
		if (items == null || items.isEmpty()) return Res.error(
			"CSV item data missing"
		);

		try {
			var data = new float[PARAMS * items.size()];

			for (var i = 0; i < items.size(); i++) {
				var item = items.get(i);
				encodeFeatures(i * PARAMS, item, data);
			}

			var matrix = new DMatrix(data, items.size(), PARAMS, Float.NaN);

			if (includeLabels) {
				var labels = new float[items.size()];
				for (var i = 0; i < items.size(); i++) {
					labels[i] = (float) items.get(i).heatDemand();
				}
				matrix.setLabel(labels);
			}

			return Res.ok(matrix);
		} catch (Exception e) {
			return Res.error("Failed to encode CSV data", e);
		}
	}

	private void encodeFeatures(int offset, CsvItem item, float[] data) {
		int p = offset;
		data[p] = (float) item.height();
		data[p + 1] = (float) item.storeys();
		data[p + 2] = (float) item.groundArea();
		var type = BuildingType.of(item.buildingTypeCode());
		data[p + 3] = FeatureValue.typeFactor(type);
		data[p + 4] = FeatureValue.climateRegionFactor(item.climateRegionCode());
		var age = ConstructionAge.ofCode(item.constructionAgeCode());
		data[p + 5] = FeatureValue.averageHeatDemand(age);
		data[p + 6] = (float) item.roofTypeFactor();
	}
}
