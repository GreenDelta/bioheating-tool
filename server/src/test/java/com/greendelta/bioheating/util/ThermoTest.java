package com.greendelta.bioheating.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ThermoTest {

	@Test
	void testMassFlowOf() {
		double massFlow = Thermo.massFlowOf(80, 50, 1512);
		assertEquals(12, massFlow, 1e-1);
	}

}
