package com.greendelta.bioheating.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WaterPropsTest {

	@Test
	void testKinematicViscosity() {
		assertEquals(1.306, WaterProps.kinematicViscosityOf(10), 1e-3);
		assertEquals(0.893, WaterProps.kinematicViscosityOf(25), 1e-3);
		assertEquals(0.554, WaterProps.kinematicViscosityOf(50), 1e-3);
		assertEquals(0.388, WaterProps.kinematicViscosityOf(75), 1e-3);
		assertEquals(0.294, WaterProps.kinematicViscosityOf(100), 1e-3);
	}

	@Test
	void testDensity() {
		assertEquals(999.65, WaterProps.densityOf(10), 1e-2);
		assertEquals(997.00, WaterProps.densityOf(25), 1e-2);
		assertEquals(988.01, WaterProps.densityOf(50), 1e-2);
		assertEquals(974.83, WaterProps.densityOf(75), 1e-2);
		assertEquals(958.35, WaterProps.densityOf(100), 1e-2);
	}

	@Test
	void testHeatCapacity() {
		assertEquals(4196d / 3600, WaterProps.heatCapacityOf(10), 1e-3);
		assertEquals(4182d / 3600, WaterProps.heatCapacityOf(25), 1e-3);
		assertEquals(4180d / 3600, WaterProps.heatCapacityOf(50), 1e-3);
		assertEquals(4192d / 3600, WaterProps.heatCapacityOf(75), 1e-3);
		assertEquals(4217d / 3600, WaterProps.heatCapacityOf(100), 1e-3);
	}

}
