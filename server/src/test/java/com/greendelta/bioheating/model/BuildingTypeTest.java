package com.greendelta.bioheating.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BuildingTypeTest {

	@Test
	void testFromCode() {

		// fall back to multi-generation
		assertEquals(BuildingType.MULTI_GENERATION, BuildingType.fromCode(0));
		assertEquals(BuildingType.MULTI_GENERATION, BuildingType.fromCode(99));
		assertEquals(10, BuildingType.MULTI_GENERATION.code());

		// map known codes
		assertEquals(BuildingType.HIGH_RISE, BuildingType.fromCode(1));
		assertEquals(BuildingType.HOUSE_GROUP, BuildingType.fromCode(9));
		assertEquals(BuildingType.MULTI_GENERATION, BuildingType.fromCode(10));
	}

	@Test
	void testEstimateLarge() {
		// a height above 25 m is always a high-rise
		assertEquals(
			BuildingType.HIGH_RISE,
			BuildingType.estimateFrom(25.1, 100, () -> 0)
		);
		assertEquals(
			BuildingType.HIGH_RISE,
			BuildingType.estimateFrom(40, 20, () -> 2)
		);

		// between 12 m and 25 m the block volume decides
		assertEquals(
			BuildingType.MULTI_FAMILY_SMALL,
			BuildingType.estimateFrom(20, 90) // volume 1800
		);
		assertEquals(
			BuildingType.MULTI_FAMILY_MEDIUM,
			BuildingType.estimateFrom(20, 150) // volume 3000
		);
		assertEquals(
			BuildingType.MULTI_FAMILY_LARGE,
			BuildingType.estimateFrom(20, 400) // volume 8000
		);
	}

	@Test
	void testEstimateByBlock() {
		// volume < 2000 -> small, == 2000 -> medium
		assertEquals(
			BuildingType.MULTI_FAMILY_SMALL,
			BuildingType.estimateFrom(20, 99.9)
		);
		assertEquals(
			BuildingType.MULTI_FAMILY_MEDIUM,
			BuildingType.estimateFrom(20, 100) // volume 2000
		);
		// volume < 5000 -> medium, == 5000 -> large
		assertEquals(
			BuildingType.MULTI_FAMILY_MEDIUM,
			BuildingType.estimateFrom(20, 249.9)
		);
		assertEquals(
			BuildingType.MULTI_FAMILY_LARGE,
			BuildingType.estimateFrom(20, 250) // volume 5000
		);
	}

	@Test
	void testLowRiseByVolumeAndArea() {
		// low-rise with a volume above 2000 m3 is a small multi-family house
		assertEquals(
			BuildingType.MULTI_FAMILY_SMALL,
			BuildingType.estimateFrom(10, 210, () -> 0) // volume 2100
		);
		// a volume between 1200 m3 and 2000 m3 is a multi-generation house
		assertEquals(
			BuildingType.MULTI_GENERATION,
			BuildingType.estimateFrom(10, 200, () -> 0) // volume 2000
		);
		assertEquals(
			BuildingType.MULTI_GENERATION,
			BuildingType.estimateFrom(10, 130, () -> 0) // volume 1300
		);
		// volume <= 1200 m3 and a ground area above 150 m2: multi-generation
		assertEquals(
			BuildingType.MULTI_GENERATION,
			BuildingType.estimateFrom(5, 200, () -> 0) // volume 1000
		);
		// a ground area below 30 m2 is a building part
		assertEquals(
			BuildingType.BUILDING_PART,
			BuildingType.estimateFrom(10, 20, () -> 0)
		);
		// in between the neighbor count decides
		assertEquals(
			BuildingType.SINGLE_FAMILY,
			BuildingType.estimateFrom(10, 100, () -> 0)
		);
		assertEquals(
			BuildingType.END_TERRACE,
			BuildingType.estimateFrom(10, 100, () -> 1)
		);
		assertEquals(
			BuildingType.MID_TERRACE,
			BuildingType.estimateFrom(10, 100, () -> 2)
		);
		assertEquals(
			BuildingType.HOUSE_GROUP,
			BuildingType.estimateFrom(10, 100, () -> 3)
		);
	}

	@Test
	void testEstimationBoundaries() {
		// height == 25 is not a high-rise, but in the multi-family branch
		assertEquals(
			BuildingType.MULTI_FAMILY_MEDIUM,
			BuildingType.estimateFrom(25, 100) // volume 2500
		);
		// height == 12 is still low-rise
		assertEquals(
			BuildingType.SINGLE_FAMILY,
			BuildingType.estimateFrom(12, 100)
		);
		// volume == 1200 is not multi-generation, it falls through
		assertEquals(
			BuildingType.SINGLE_FAMILY,
			BuildingType.estimateFrom(10, 120)
		);
		// area == 150 is not multi-generation, it falls through to the neighbors
		assertEquals(
			BuildingType.SINGLE_FAMILY,
			BuildingType.estimateFrom(7, 150)
		);
		// volume == 2000 is not a small multi-family house
		assertEquals(
			BuildingType.MULTI_GENERATION,
			BuildingType.estimateFrom(10, 200)
		);
		// area == 30 is not a building part, it falls through to the neighbors
		assertEquals(
			BuildingType.SINGLE_FAMILY,
			BuildingType.estimateFrom(10, 30)
		);
		assertEquals(
			BuildingType.BUILDING_PART,
			BuildingType.estimateFrom(10, 29.9)
		);
	}

	@Test
	void testEstimateWithMissingValues() {
		// nothing provided -> only the neighbors decide
		assertEquals(
			BuildingType.SINGLE_FAMILY,
			BuildingType.estimateFrom(0, 0, () -> 0)
		);
		assertEquals(
			BuildingType.MID_TERRACE,
			BuildingType.estimateFrom(0, 0, () -> 2)
		);
		assertEquals(
			BuildingType.HOUSE_GROUP,
			BuildingType.estimateFrom(0, 0, () -> 4)
		);

		// negative values count as not provided
		assertEquals(
			BuildingType.END_TERRACE,
			BuildingType.estimateFrom(-5, -5, () -> 1)
		);

		// only the ground area is known
		assertEquals(
			BuildingType.MULTI_FAMILY_MEDIUM,
			BuildingType.estimateFrom(-1, 200)
		);
		assertEquals(
			BuildingType.BUILDING_PART,
			BuildingType.estimateFrom(-1, 20)
		);
		assertEquals(
			BuildingType.SINGLE_FAMILY,
			BuildingType.estimateFrom(-1, 100)
		);

		// only the height is known
		assertEquals(
			BuildingType.HIGH_RISE,
			BuildingType.estimateFrom(30, 0)
		);
		assertEquals(
			BuildingType.MULTI_FAMILY_LARGE,
			BuildingType.estimateFrom(20, 0)
		);
		assertEquals(
			BuildingType.SINGLE_FAMILY,
			BuildingType.estimateFrom(8, -1)
		);
	}

	@Test
	void testDefaultsAreConsistentWithEstimation() {
		assertRoundTrip(BuildingType.HIGH_RISE, 0);
		assertRoundTrip(BuildingType.MULTI_FAMILY_SMALL, 0);
		assertRoundTrip(BuildingType.MULTI_FAMILY_MEDIUM, 0);
		assertRoundTrip(BuildingType.MULTI_FAMILY_LARGE, 0);
		assertRoundTrip(BuildingType.BUILDING_PART, 0);
		assertRoundTrip(BuildingType.SINGLE_FAMILY, 0);
		assertRoundTrip(BuildingType.END_TERRACE, 1);
		assertRoundTrip(BuildingType.MID_TERRACE, 2);
		assertRoundTrip(BuildingType.HOUSE_GROUP, 3);
		assertRoundTrip(BuildingType.MULTI_GENERATION, 0);
	}

	/// When only the ground area or only the height is known, the missing value
	/// is later filled with the default of the estimated type. The full
	/// decision tree must then give the same type again.
	@Test
	void testMissingValuesAreConsistentWithDefaults() {
		double[] areas = {10, 29.9, 30, 100, 149.9, 150, 200, 332.9, 333, 1000};
		double[] heights = {3, 8, 12, 12.1, 16.5, 16.6, 20, 25, 25.1, 40};
		for (int neighbors = 0; neighbors <= 3; neighbors++) {
			int n = neighbors;
			for (double area : areas) {
				var type = BuildingType.estimateFrom(0, area, () -> n);
				assertEquals(
					type,
					BuildingType.estimateFrom(type.defaultHeight(), area, () -> n),
					"ground area " + area + " m2 with the default height of " + type
				);
			}
			for (double height : heights) {
				var type = BuildingType.estimateFrom(height, 0, () -> n);
				assertEquals(
					type,
					BuildingType.estimateFrom(height, type.defaultGroundArea(), () -> n),
					"height " + height + " m with the default ground area of " + type
				);
			}
		}
	}

	/// The default height and ground area of a type must estimate back to that
	/// type with the given number of heated neighbors.
	private static void assertRoundTrip(BuildingType type, int neighbors) {
		assertEquals(
			type,
			BuildingType.estimateFrom(
				type.defaultHeight(),
				type.defaultGroundArea(),
				() -> neighbors
			),
			"default height and ground area of " + type
				+ " must estimate back to that type"
		);
	}
}
