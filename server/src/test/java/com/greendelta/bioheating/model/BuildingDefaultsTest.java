package com.greendelta.bioheating.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BuildingDefaultsTest {

	@Test
	void keepsProvidedValues() {
		var defaults = BuildingDefaults.resolve(
			BuildingType.HIGH_RISE, 40, 500, 0);
		assertEquals(BuildingType.HIGH_RISE, defaults.type());
		assertEquals(40, defaults.height(), 1e-6);
		assertEquals(500, defaults.groundArea(), 1e-6);
	}

	@Test
	void fillsMissingGeometryFromTheType() {
		var defaults = BuildingDefaults.resolve(
			BuildingType.HIGH_RISE, 0, 0, 0);
		assertEquals(BuildingType.HIGH_RISE, defaults.type());
		assertEquals(
			BuildingType.HIGH_RISE.defaultHeight(),
			defaults.height(),
			1e-6
		);
		assertEquals(
			BuildingType.HIGH_RISE.defaultGroundArea(),
			defaults.groundArea(),
			1e-6
		);
	}

	@Test
	void treatsNonPositiveValuesAsNotProvided() {
		var defaults = BuildingDefaults.resolve(
			BuildingType.SINGLE_FAMILY, -1, 0, 0);
		assertEquals(BuildingType.SINGLE_FAMILY, defaults.type());
		assertEquals(
			BuildingType.SINGLE_FAMILY.defaultHeight(),
			defaults.height(),
			1e-6
		);
		assertEquals(
			BuildingType.SINGLE_FAMILY.defaultGroundArea(),
			defaults.groundArea(),
			1e-6
		);
	}

	@Test
	void estimatesTheTypeFromTheGeometry() {
		// a height above 25 m is a high-rise, provided values are kept
		var highRise = BuildingDefaults.resolve(null, 30, 100, 0);
		assertEquals(BuildingType.HIGH_RISE, highRise.type());
		assertEquals(30, highRise.height(), 1e-6);
		assertEquals(100, highRise.groundArea(), 1e-6);

		// a block volume of 2000 is a medium multi-family house
		var medium = BuildingDefaults.resolve(null, 20, 100, 0);
		assertEquals(BuildingType.MULTI_FAMILY_MEDIUM, medium.type());
		assertEquals(20, medium.height(), 1e-6);
		assertEquals(100, medium.groundArea(), 1e-6);
	}

	@Test
	void estimatesTypeAndGeometryWhenNothingIsProvided() {
		var single = BuildingDefaults.resolve(null, 0, 0, 0);
		assertEquals(BuildingType.SINGLE_FAMILY, single.type());
		assertEquals(
			BuildingType.SINGLE_FAMILY.defaultHeight(),
			single.height(),
			1e-6
		);
		assertEquals(
			BuildingType.SINGLE_FAMILY.defaultGroundArea(),
			single.groundArea(),
			1e-6
		);

		var terrace = BuildingDefaults.resolve(null, -1, -1, 1);
		assertEquals(BuildingType.END_TERRACE, terrace.type());
		assertEquals(
			BuildingType.END_TERRACE.defaultHeight(),
			terrace.height(),
			1e-6
		);
		assertEquals(
			BuildingType.END_TERRACE.defaultGroundArea(),
			terrace.groundArea(),
			1e-6
		);
	}

	@Test
	void keepsProvidedValuesWhenTheTypeIsGiven() {
		var defaults = BuildingDefaults.resolve(
			BuildingType.MULTI_GENERATION, 0, 200, 0);
		assertEquals(BuildingType.MULTI_GENERATION, defaults.type());
		assertEquals(
			BuildingType.MULTI_GENERATION.defaultHeight(),
			defaults.height(),
			1e-6
		);
		assertEquals(200, defaults.groundArea(), 1e-6);
	}
}
