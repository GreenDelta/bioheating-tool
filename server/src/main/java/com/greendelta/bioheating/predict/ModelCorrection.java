package com.greendelta.bioheating.predict;

import java.util.Objects;

/// Holds the corrections that are applied to the raw outputs of the prediction
/// model. When nothing is configured, the outputs are not changed.
public final class ModelCorrection {

	private static volatile Correction heatDemand = Correction.NONE;
	private static volatile Correction peakLoad = Correction.NONE;

	private ModelCorrection() {}

	/// The correction of the given target.
	static Correction of(Target target) {
		return switch (target) {
			case HEAT_DEMAND -> heatDemand;
			case PEAK_LOAD -> peakLoad;
		};
	}

	/// Sets the corrections for the annual heat demand and the peak load.
	public static void configure(Correction heatDemand, Correction peakLoad) {
		ModelCorrection.heatDemand = Objects.requireNonNull(heatDemand);
		ModelCorrection.peakLoad = Objects.requireNonNull(peakLoad);
	}
}
