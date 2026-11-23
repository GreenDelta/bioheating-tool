package com.greendelta.bioheating.predict;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CorrectionTest {

	@Test
	void appliesSlopeAndOffset() {
		assertEquals(30.0, new Correction(2, 10).apply(10));
	}

	@Test
	void noneDoesNotChangeTheValue() {
		assertEquals(42.5, Correction.NONE.apply(42.5));
	}

	@Test
	void neverReturnsNegativeValues() {
		assertEquals(0.0, new Correction(1, -100).apply(10));
	}

	@Test
	void selectsTheCorrectionOfTheTarget() {
		var heatDemand = new Correction(2, 1);
		var peakLoad = new Correction(3, 4);
		var config = new ModelConfig(null, heatDemand, peakLoad);
		assertEquals(heatDemand, config.correctionOf(Target.HEAT_DEMAND));
		assertEquals(peakLoad, config.correctionOf(Target.PEAK_LOAD));
	}

	@Test
	void noCorrectionsByDefault() {
		var config = ModelConfig.NONE;
		assertEquals(Correction.NONE, config.correctionOf(Target.HEAT_DEMAND));
		assertEquals(Correction.NONE, config.correctionOf(Target.PEAK_LOAD));
	}
}
