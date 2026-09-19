package com.greendelta.bioheating.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ConstructionAgeTest {

	@Test
	void fallsBackToAge1979To1995() {
		assertEquals(ConstructionAge.AGE_1979_1995, ConstructionAge.ofCode(0));
		assertEquals(ConstructionAge.AGE_1979_1995, ConstructionAge.ofCode(99));
		assertEquals(
			ConstructionAge.AGE_1979_1995,
			ConstructionAge.fromString("unknown")
		);
		assertEquals(
			ConstructionAge.AGE_1979_1995,
			ConstructionAge.fromString(null)
		);
	}

	@Test
	void mapsKnownValues() {
		assertEquals(ConstructionAge.AGE_1949_1978, ConstructionAge.ofCode(3));
		assertEquals(
			ConstructionAge.AGE_2010_2030,
			ConstructionAge.fromString("2010-2030")
		);
		assertEquals(
			ConstructionAge.AGE_1979_1995,
			ConstructionAge.ofYear(1985)
		);
	}
}
