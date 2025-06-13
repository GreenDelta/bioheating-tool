package com.greendelta.bioheating.io;

import java.util.List;

import com.greendelta.bioheating.model.Building;

import ml.dmlc.xgboost4j.java.DMatrix;

class BoostEncoder {

	private static final int PARAMS = 5;

	static DMatrix encodeBuildingData(List<Building> items) {
		try {
			var data = new float[items.size() * PARAMS];
			for (var i = 0; i < items.size(); i++) {
				var item = items.get(i);
				System.arraycopy(encode(item), 0, data, i * PARAMS, PARAMS);
			}
			return new DMatrix(data, items.size(), 5, Float.NaN);
		} catch (Exception e) {
			throw new RuntimeException("failed to encode building data", e);
		}
	}

	static float[] encode(Building b) {
		return new float[] {
			(float) b.height(),
			(float) b.storeys(),
			encodeClimateZone(b.weatherStation()),
			(float) b.volume(),
			(float) b.heatedArea(),
		};
	}

	private static float encodeClimateZone(int zone) {
		return switch (zone) {
			case 1 -> 0.85f;
			case 2 -> 1.08f;
			case 3 -> 0.83f;
			case 4, 8 -> 0.84f;
			case 5 -> 1.01f;
			case 6 -> 0.8f;
			case 7, 10 -> 1.06f;
			case 9, 15 -> 0.89f;
			case 11 -> 1.16f;
			case 12 -> 0.91f;
			case 13 -> 1.15f;
			case 14 -> 1.19f;
			default -> 1.0f;
		};
	}
}
