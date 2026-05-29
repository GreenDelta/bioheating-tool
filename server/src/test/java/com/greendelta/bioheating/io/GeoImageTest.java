package com.greendelta.bioheating.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;

class GeoImageTest {

	@Test
	void usesMaxWidthForWideEnvelopes() {
		try (var image = new GeoImage(1024, new Envelope(0, 200, 0, 100))) {
			assertEquals(1024, image.getImage().getWidth());
			assertEquals(512, image.getImage().getHeight());
		}
	}

	@Test
	void usesMaxHeightForTallEnvelopes() {
		try (var image = new GeoImage(1024, new Envelope(0, 100, 0, 200))) {
			assertEquals(512, image.getImage().getWidth());
			assertEquals(1024, image.getImage().getHeight());
		}
	}
}
