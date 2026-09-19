package com.greendelta.bioheating.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BuildingTypeTest {

	@Test
	void fallsBackToMultiGeneration() {
		assertEquals(BuildingType.MULTI_GENERATION, BuildingType.of(0));
		assertEquals(BuildingType.MULTI_GENERATION, BuildingType.of(99));
		assertEquals(10, BuildingType.MULTI_GENERATION.code());
	}

	@Test
	void mapsKnownCodes() {
		assertEquals(BuildingType.HIGH_RISE, BuildingType.of(1));
		assertEquals(BuildingType.HOUSE_GROUP, BuildingType.of(9));
		assertEquals(BuildingType.MULTI_GENERATION, BuildingType.of(10));
	}
}
