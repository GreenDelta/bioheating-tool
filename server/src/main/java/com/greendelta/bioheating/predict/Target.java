package com.greendelta.bioheating.predict;

/// The two targets that are predicted by the model. Each target is trained as
/// its own booster and stored in its own model file.
public enum Target {

	/// The annual heat demand of a building in kWh.
	HEAT_DEMAND("demand-model.ubj"),

	/// The peak heating load of a building in kW.
	PEAK_LOAD("peak-model.ubj");

	private final String modelFile;

	Target(String modelFile) {
		this.modelFile = modelFile;
	}

	/// The name of the model file of this target.
	public String modelFile() {
		return modelFile;
	}

	/// The expected value of this target in the given data row.
	public double labelOf(CsvItem item) {
		return switch (this) {
			case HEAT_DEMAND -> item.heatDemand();
			case PEAK_LOAD -> item.peakLoad();
		};
	}
}
