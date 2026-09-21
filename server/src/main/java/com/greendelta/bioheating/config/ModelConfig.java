package com.greendelta.bioheating.config;

import com.greendelta.bioheating.predict.Correction;
import com.greendelta.bioheating.predict.ModelCorrection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/// Configures the linear corrections that are applied to the outputs of the
/// prediction model: `y = a * x + b`.
@Configuration
public class ModelConfig {

	public ModelConfig(
		@Value("${bioheating.model.heat-demand.a:1}") double heatDemandA,
		@Value("${bioheating.model.heat-demand.b:0}") double heatDemandB,
		@Value("${bioheating.model.peak-load.a:1}") double peakLoadA,
		@Value("${bioheating.model.peak-load.b:0}") double peakLoadB
	) {
		ModelCorrection.configure(
			new Correction(heatDemandA, heatDemandB),
			new Correction(peakLoadA, peakLoadB)
		);
	}
}
