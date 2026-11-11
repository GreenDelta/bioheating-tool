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
	void addsTheWarmWaterShareToTheDemand() {
		var building = new Building().warmWaterFraction(14);
		assertEquals(
			1162.7906976744187,
			WarmWater.addToDemand(building, 1000),
			1e-9
		);
	}

	@Test
	void addsTheWarmWaterBaseLoadToThePeakLoad() {
		// 20 MWh space heating with 14% warm water: 23,255.81 kWh total, thus
		// 3,255.81 kWh warm water or a constant base load of 0.3717 kW
		var building = new Building().warmWaterFraction(14);
		var peak = WarmWater.addToPeakLoad(building, 20_000.0 / 1400, 20_000);
		assertEquals(20_000.0 / 1400 + (20_000 / 0.86 - 20_000) / 8760, peak, 1e-9);
		assertEquals(14.6574, peak, 1e-4);
	}

	@Test
	void addsNothingWithoutAValidWarmWaterFraction() {
		for (var building : new Building[] {
			null,
			new Building(),
			new Building().warmWaterFraction(100),
		}) {
			assertEquals(1000.0, WarmWater.addToDemand(building, 1000), 1e-9);
			assertEquals(
				10.0,
				WarmWater.addToPeakLoad(building, 10, 1000),
				1e-9
			);
		}
	}
}
