package com.greendelta.bioheating.predict;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CorrectionTest {

	@AfterEach
	void resetCorrections() {
		ModelCorrection.configure(Correction.NONE, Correction.NONE);
	}

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
	void defaultsToNoCorrection() {
		assertEquals(Correction.NONE, ModelCorrection.of(Target.HEAT_DEMAND));
		assertEquals(Correction.NONE, ModelCorrection.of(Target.PEAK_LOAD));
	}

	@Test
	void configuresEachTargetSeparately() {
		var heatDemand = new Correction(2, 1);
		var peakLoad = new Correction(3, 4);
		ModelCorrection.configure(heatDemand, peakLoad);
		assertEquals(heatDemand, ModelCorrection.of(Target.HEAT_DEMAND));
		assertEquals(peakLoad, ModelCorrection.of(Target.PEAK_LOAD));
	}
}
