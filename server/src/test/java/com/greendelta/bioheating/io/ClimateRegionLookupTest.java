package com.greendelta.bioheating.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

public class ClimateRegionLookupTest {

	@Test
	public void testLookupContainsCoordinate() {
		// Inside region 14 bounds (Freiburg / Feldberg area in south Germany)
		Coordinate insideRegion14 = new Coordinate(8.30, 47.85);
		int region = ClimateRegionLookup.lookup(insideRegion14);
		assertEquals(14, region);
	}

	@Test
	public void testLookupNearestFallback() {
		// A coordinate outside the data boundaries, but closest to region 12
		Coordinate nearRegion12 = new Coordinate(8.25, 47.50);
		int region = ClimateRegionLookup.lookup(nearRegion12);
		assertEquals(12, region);
	}
}
