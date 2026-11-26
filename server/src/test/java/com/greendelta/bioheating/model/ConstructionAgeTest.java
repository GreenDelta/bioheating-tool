package com.greendelta.bioheating.model;

import static org.junit.jupiter.api.Assertions.*;

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

	/// The classes do not overlap: each year at a class border belongs to
	/// exactly one class.
	@Test
	void testClassBorders() {
		assertEquals(ConstructionAge.AGE_1900_1918, ConstructionAge.ofYear(1918));
		assertEquals(ConstructionAge.AGE_1919_1948, ConstructionAge.ofYear(1919));
		assertEquals(ConstructionAge.AGE_1919_1948, ConstructionAge.ofYear(1948));
		assertEquals(ConstructionAge.AGE_1949_1978, ConstructionAge.ofYear(1949));
		assertEquals(ConstructionAge.AGE_1949_1978, ConstructionAge.ofYear(1978));
		assertEquals(ConstructionAge.AGE_1979_1995, ConstructionAge.ofYear(1979));
		assertEquals(ConstructionAge.AGE_1979_1995, ConstructionAge.ofYear(1995));
		assertEquals(ConstructionAge.AGE_1996_2009, ConstructionAge.ofYear(1996));
		assertEquals(ConstructionAge.AGE_1996_2009, ConstructionAge.ofYear(2009));
		assertEquals(ConstructionAge.AGE_2010_2030, ConstructionAge.ofYear(2010));
	}

	@Test
	void parsesUserProvidedValues() {
		// codes
		assertEquals(ConstructionAge.AGE_1949_1978, ConstructionAge.parse("3"));
		assertEquals(
			ConstructionAge.AGE_1979_1995,
			ConstructionAge.parse("4.0")
		);
		// years
		assertEquals(
			ConstructionAge.AGE_1979_1995,
			ConstructionAge.parse("1994")
		);
		assertEquals(
			ConstructionAge.AGE_1996_2009,
			ConstructionAge.parse("2000")
		);
		// range strings (the ranges are defined by their upper bound)
		assertEquals(
			ConstructionAge.AGE_1900_1918,
			ConstructionAge.parse("1900-1918")
		);
		assertEquals(
			ConstructionAge.AGE_1919_1948,
			ConstructionAge.parse("1919 - 1948")
		);
		assertEquals(
			ConstructionAge.AGE_1949_1978,
			ConstructionAge.parse("1949-1978")
		);
		assertEquals(
			ConstructionAge.AGE_1979_1995,
			ConstructionAge.parse("1979-1994")
		);
		assertEquals(
			ConstructionAge.AGE_1996_2009,
			ConstructionAge.parse("1996-2009")
		);
		assertEquals(
			ConstructionAge.AGE_2010_2030,
			ConstructionAge.parse("2010-2030")
		);
	}

	@Test
	void returnsNullForUnparseableValues() {
		assertNull(ConstructionAge.parse(null));
		assertNull(ConstructionAge.parse(""));
		assertNull(ConstructionAge.parse("  "));
		assertNull(ConstructionAge.parse("unknown"));
		assertNull(ConstructionAge.parse("-5"));
		assertNull(ConstructionAge.parse("0"));
	}
}
