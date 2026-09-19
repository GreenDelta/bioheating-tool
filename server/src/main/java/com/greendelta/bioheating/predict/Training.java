package com.greendelta.bioheating.predict;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.XGBoost;
import org.openlca.commons.Res;

/// Trains the heat demand and the peak load model from a CSV file.
public class Training {

	private final List<CsvItem> items;

	private Training(List<CsvItem> items) {
		this.items = Objects.requireNonNull(items);
	}

	/// Trains both models from the data of the given CSV file.
	public static Res<Models> trainFrom(File csv) {
		var items = CsvItem.readFrom(csv);
		if (items.isError()) return items.wrapError(
			"Failed to read training data"
		);

		var training = new Training(items.value());
		var heatDemand = training.train(Target.HEAT_DEMAND);
		if (heatDemand.isError()) return heatDemand.castError();
		var peakLoad = training.train(Target.PEAK_LOAD);
		if (peakLoad.isError()) return peakLoad.castError();

		return Res.ok(new Models(heatDemand.value(), peakLoad.value()));
	}

	/// Writes both models into the given directory, using the file names of
	/// their targets.
	public static Res<Void> save(Models models, File dir) {
		for (var target : Target.values()) {
			var res = save(models.of(target), new File(dir, target.modelFile()));
			if (res.isError()) return res;
		}
		return Res.ok();
	}

	private Res<Booster> train(Target target) {
		try {
			var data = CsvEncoder.encodeWithLabels(items, target);
			if (data.isError()) return data.wrapError(
				"Failed to encode training data"
			);
			var model = XGBoost.train(
				data.value(),
				config(),
				1000,
				new HashMap<>(),
				null,
				null
			);
			return Res.ok(model);
		} catch (Exception e) {
			return Res.error("Failed to train model", e);
		}
	}

	private static HashMap<String, Object> config() {
		var config = new HashMap<String, Object>();
		config.put("objective", "reg:squarederror"); // regression task
		config.put("tree_method", "hist");
		config.put("reg_alpha", 0.1);
		config.put("eta", 0.5); // learning rate
		config.put("max_depth", 6); // maximum tree depth
		return config;
	}

	private static Res<Void> save(Booster booster, File file) {
		try {
			Files.write(file.toPath(), booster.toByteArray("ubj"));
			return Res.ok();
		} catch (Exception e) {
			return Res.error("Failed to save model to file: " + file, e);
		}
	}

	/// The two trained models.
	///
	/// @param heatDemand the model for the annual heat demand in kWh
	/// @param peakLoad the model for the peak heating load in kW
	public record Models(Booster heatDemand, Booster peakLoad) {

		/// The model of the given target.
		public Booster of(Target target) {
			return switch (target) {
				case HEAT_DEMAND -> heatDemand;
				case PEAK_LOAD -> peakLoad;
			};
		}
	}
}
