package com.greendelta.bioheating.pipes;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PipesTest {

	@Test
	void testMassFlowOf() {
		double massFlow = Pipes.massFlowOf(80, 50, 1512);
		assertEquals(12, massFlow, 1e-1);
	}

	@Test
	void testFlowVelocityOf() {
		double velocity = Pipes.flowVelocityOf(12, 0.1, 65);
		assertEquals(1.55, velocity, 1e-2);
	}

	@Test
	void testPressureLossOf() {
		// velocity 1.55 m/s, diameter 0.1 m, roughness 0.01 mm (steel), temp 65°C
		double pressureLoss = Pipes.pressureLossOf(1.55, 0.1, 0.01e-3, 65);
		assertEquals(170.4, pressureLoss, 1e-1);

		// https://www.npro.energy/main/de/help/pipe-dimensioning
		var v = Pipes.flowVelocityOf(12.5, 90e-3, 60);
		assertEquals(300, Pipes.pressureLossOf(v, 90e-3, 0.007e-3, 60), 10);
	}

}
