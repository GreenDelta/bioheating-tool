package com.greendelta.bioheating.io;

import static com.greendelta.bioheating.io.ClimateRegionLookup.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

public class ClimateRegionLookupTest {

	@Test
	public void testRegions() {
		assertEquals(1, lookup(new Coordinate(8.577848, 53.55683))); // Bremerhaven
		assertEquals(2, lookup(new Coordinate(11.8173429, 54.1475029))); // Rostock
		assertEquals(3, lookup(new Coordinate(10.0042712, 53.5517552))); // Hamburg
		assertEquals(4, lookup(new Coordinate(12.6978693, 52.4283252))); // Potsdam
		assertEquals(5, lookup(new Coordinate(6.9334256, 51.4409755))); // Essen
		assertEquals(6, lookup(new Coordinate(7.9287774, 50.6486843))); // Bad Marienberg
		assertEquals(7, lookup(new Coordinate(9.3780433, 51.3149644))); // Kassel
		assertEquals(8, lookup(new Coordinate(10.6142657, 51.7996027))); // Braunlage
		assertEquals(9, lookup(new Coordinate(12.8082096, 50.822734))); // Chemnitz
		assertEquals(10, lookup(new Coordinate(11.8479144, 50.3112868))); // Hof
		assertEquals(11, lookup(new Coordinate(12.9442557, 50.4287042))); // Fichtelberg
		assertEquals(12, lookup(new Coordinate(8.4195035, 49.5005078))); // Mannheim
		assertEquals(13, lookup(new Coordinate(13.3669541, 48.576982))); // Passau
		assertEquals(14, lookup(new Coordinate(9.818851, 48.6253098))); // Stoetten
		assertEquals(15, lookup(new Coordinate(10.963643, 47.4712373))); // Garmisch Partenkirchen
	}

	@Test
	public void testNearest() {
		assertEquals(4, lookup(new Coordinate(14.9122362, 52.5612158))); // Poland
		assertEquals(11, lookup(new Coordinate(13.1275149, 50.3572464))); // Czechia
		assertEquals(15, lookup(new Coordinate(12.0053722, 47.4453105))); // Austria
	}
}
