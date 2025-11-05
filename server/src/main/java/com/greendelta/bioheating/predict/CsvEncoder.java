package com.greendelta.bioheating.predict;

import java.util.List;

import com.greendelta.bioheating.model.BuildingType;
import com.greendelta.bioheating.model.ConstructionAge;
import com.greendelta.bioheating.util.Res;

import ml.dmlc.xgboost4j.java.DMatrix;

class CsvEncoder {

	private static final int PARAMS = 7;

	private final List<CsvItem> items;
	private final float[] data;

	private CsvEncoder(List<CsvItem> items) {
		this.items = items;
		this.data = new float[PARAMS * items.size()];
	}

	static Res<DMatrix> encode(List<CsvItem> items) {
		if (items == null || items.isEmpty())
			return Res.error("CSV item data missing");
		return new CsvEncoder(items).run();
	}

	private Res<DMatrix> run() {
		try {
			for (var i = 0; i < items.size(); i++) {
				var item = items.get(i);
				encode(i, item);
			}
			var matrix = new DMatrix(data, items.size(), PARAMS, Float.NaN);
			return Res.of(matrix);
		} catch (Exception e) {
			return Res.error("Failed to encode CSV data", e);
		}
	}

	private void encode(int offset, CsvItem item) {
		int p = offset * PARAMS;
		data[p] = (float) item.height();
		data[p + 1] = (float) item.storeys();
		data[p + 2] = (float) item.groundArea();
		data[p + 3] = FeatureValue.typeFactor(BuildingType.of(item.buildingTypeCode()));
		data[p + 4] = FeatureValue.climateRegionFactor(item.climateRegionCode());
		data[p + 5] = FeatureValue.averageHeatDemand(ConstructionAge.ofCode(item.constructionAgeCode()));
		data[p + 6] = (float) item.roofTypeFactor();
	}
}
