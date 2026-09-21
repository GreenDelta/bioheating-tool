package com.greendelta.bioheating.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WarmWaterTest {

	@Test
	void looksUpTheFractionForTheTypeAndAge() {
		assertEquals(
			22.0,
			WarmWater.of(BuildingType.HIGH_RISE, ConstructionAge.AGE_1900_1919),
			1e-6
		);
		assertEquals(
			43.0,
			WarmWater.of(BuildingType.HIGH_RISE, ConstructionAge.AGE_2010_2030),
			1e-6
		);
		assertEquals(
			9.0,
			WarmWater.of(BuildingType.SINGLE_FAMILY, ConstructionAge.AGE_1900_1919),
			1e-6
		);
		assertEquals(
			18.5,
			WarmWater.of(BuildingType.END_TERRACE, ConstructionAge.AGE_1979_1995),
			1e-6
		);
		assertEquals(
			36.0,
			WarmWater.of(
				BuildingType.MULTI_GENERATION,
				ConstructionAge.AGE_2010_2030
			),
			1e-6
		);
	}

	@Test
	void usesTheDefaultsWhenTypeOrAgeIsMissing() {
		assertEquals(
			WarmWater.of(
				BuildingType.MULTI_GENERATION,
				ConstructionAge.AGE_1919_1948
			),
			WarmWater.of(null, ConstructionAge.AGE_1919_1948),
			1e-6
		);
		assertEquals(
			WarmWater.of(
				BuildingType.SINGLE_FAMILY,
				ConstructionAge.AGE_1979_1995
			),
			WarmWater.of(BuildingType.SINGLE_FAMILY, null),
			1e-6
		);
	}

	@Test
	void definesAFractionForEveryTypeAndAge() {
		for (var type : BuildingType.values()) {
			for (var age : ConstructionAge.values()) {
				assertTrue(
					WarmWater.of(type, age) > 0,
					"no warm water fraction defined for " + type + "/" + age
				);
			}
		}
	}

	@Test
	void scalesSpaceHeatingValuesWithTheWarmWaterFraction() {
		var building = new Building().warmWaterFraction(14);
		assertEquals(1162.7906976744187, WarmWater.totalOf(building, 1000), 1e-9);
	}

	@Test
	void doesNotScaleWithoutAWarmWaterFraction() {
		assertEquals(1000.0, WarmWater.totalOf(new Building(), 1000), 1e-9);
		assertEquals(
			1000.0,
			WarmWater.totalOf(new Building().warmWaterFraction(100), 1000),
			1e-9
		);
		assertEquals(1000.0, WarmWater.totalOf(null, 1000), 1e-9);
	}
}
