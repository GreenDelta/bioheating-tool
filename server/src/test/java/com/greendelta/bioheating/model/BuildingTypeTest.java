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
	void testSmallAndByNeighbors() {
		// a ground area above 150 m2 is a small multi-family house
		assertEquals(
			BuildingType.MULTI_FAMILY_SMALL,
			BuildingType.estimateFrom(10, 200, () -> 0)
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
		// area == 150 is not small multi-family, it falls through to the neighbors
		assertEquals(
			BuildingType.SINGLE_FAMILY,
			BuildingType.estimateFrom(10, 150)
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
	void testOldCompatibility() {
		double[] heights = {6, 12, 12.1, 20, 25, 25.1, 30};
		double[] areas = {20, 30, 30.1, 100, 150, 150.1, 250, 400};
		for (var height : heights) {
			for (var area : areas) {
				for (int neighbors = 0; neighbors < 5; neighbors++) {
					int n = neighbors;
					assertEquals(
						legacyTypeFrom(height, area, neighbors),
						BuildingType.estimateFrom(height, area, () -> n),
						"height=" + height + ", area=" + area
							+ ", neighbors=" + neighbors
					);
				}
			}
		}
	}

	/// This was the logic of the type detection in the old building processor
	/// that we ported from the initial Matlab code.
	private static BuildingType legacyTypeFrom(
		double height, double groundArea, int neighborCount
	) {
		if (height > 25)
			return BuildingType.HIGH_RISE;
		if (height > 12) {
			var blockVolume = height * groundArea;
			if (blockVolume < 2000)
				return BuildingType.MULTI_FAMILY_SMALL;
			return blockVolume < 5000
				? BuildingType.MULTI_FAMILY_MEDIUM
				: BuildingType.MULTI_FAMILY_LARGE;
		}
		if (groundArea > 150)
			return BuildingType.MULTI_FAMILY_SMALL;
		if (groundArea < 30)
			return BuildingType.BUILDING_PART;

		return switch (neighborCount) {
			case 0 -> BuildingType.SINGLE_FAMILY;
			case 1 -> BuildingType.END_TERRACE;
			case 2 -> BuildingType.MID_TERRACE;
			default -> BuildingType.HOUSE_GROUP;
		};
	}
}
