package com.greendelta.bioheating.predict;

import java.util.List;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.ClimateRegion;
import com.greendelta.bioheating.util.Res;

import ml.dmlc.xgboost4j.java.DMatrix;

class BoostEncoder {

	private static final int PARAMS = 5;

	private final float regionParam;
	private final List<Building> buildings;
	private final float[] data;

	private BoostEncoder(float regionParam, List<Building> buildings) {
		this.regionParam = regionParam;
		this.buildings = buildings;
		this.data = new float[PARAMS * buildings.size()];
	}

	static Res<DMatrix> encode(
		ClimateRegion region, List<Building> buildings) {
		if (region == null || buildings == null || buildings.isEmpty())
			return Res.error("Climate region or building data missing");

		float regionParam = FeatureValue.climateRegionFactor(region.number());
		return new BoostEncoder(regionParam, buildings).run();
	}

	private Res<DMatrix> run() {
		try {
			for (var i = 0; i < buildings.size(); i++) {
				var b = buildings.get(i);
				encode(i, b);
			}
			var matrix = new DMatrix(data, buildings.size(), PARAMS, Float.NaN);
			return Res.of(matrix);
		} catch (Exception e) {
			return Res.error("Failed to encode building data", e);
		}
	}

	private void encode(int offset, Building b) {
		int p = offset;
		data[p] = (float) b.height();
		data[p+1] = (float) b.storeys();
		data[p+2] = (float) b.groundArea();
		data[p + 3] = FeatureValue.typeFactor(b.type());
		data[p+4] = regionParam;
		// TODO: other parameters
	}
}
