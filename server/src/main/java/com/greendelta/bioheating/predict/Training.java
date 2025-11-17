package com.greendelta.bioheating.predict;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import com.greendelta.bioheating.util.Res;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.XGBoost;

public class Training {

	private final List<CsvItem> items;

	private Training(List<CsvItem> items) {
		this.items = Objects.requireNonNull(items);
	}

	public static Res<Booster> trainFrom(File csv) {
		var items = CsvItem.readFrom(csv);
		return items.hasError()
			? items.wrapError("Failed to read training data")
			: new Training(items.value()).train();
	}

	private Res<Booster> train() {
		try {
			var data = CsvEncoder.withLabels().encode(items);
			if (data.hasError())
				return data.wrapError("Failed to encode training data");
			var config = getConfig();
			var model = XGBoost.train(
				data.value(), config, 1000, new HashMap<>(), null, null);
			return Res.of(model);
		} catch (Exception e) {
			return Res.error("Failed to train model", e);
		}
	}

	private HashMap<String, Object> getConfig() {
		var config = new HashMap<String, Object>();
		config.put("objective", "reg:squarederror"); // Regression task
		config.put("tree_method", "hist");
		config.put("reg_alpha", 0.1);
		config.put("eta", 0.5); // Learning rate
		config.put("max_depth", 6); // Maximum tree depth
		return config;
	}

	public static Res<Void> save(Booster booster, File file) throws IOException {
		try {
			byte[] ubj = booster.toByteArray("ubj");
			Files.write(file.toPath(), ubj);
			return Res.VOID;
		} catch (Exception e) {
			return Res.error("Failed to save model to file: " + file, e);
		}
	}
}
