package com.greendelta.bioheating.services;

import com.greendelta.bioheating.config.DataDir;
import com.greendelta.bioheating.predict.BoostPredictor;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/// Loads the prediction model from the `model` folder of the data directory
/// once at startup. When the model cannot be loaded, the application does not
/// start.
@Service
public class ModelService {

	private final BoostPredictor predictor;

	public ModelService(DataDir dataDir) {
		var dir = dataDir.model();
		var res = BoostPredictor.load(dir);
		if (res.isError()) {
			throw new IllegalStateException(
				"Failed to load the prediction model from " + dir.getAbsolutePath()
					+ ": " + res.error()
					+ " (provide demand-model.ubj, peak-model.ubj, and config.json"
					+ " in this folder or set APP_DATA_DIR)"
			);
		}
		predictor = res.value();
		var config = predictor.config();

		var log = LoggerFactory.getLogger(getClass());
		log.info(
			"Loaded prediction model from {} ({}); corrections: heat demand {},"
				+ " peak load {}",
			dir.getAbsolutePath(),
			config.description() != null ? config.description() : "no description",
			config.heatDemand(),
			config.peakLoad()
		);
	}

	public BoostPredictor predictor() {
		return predictor;
	}
}
