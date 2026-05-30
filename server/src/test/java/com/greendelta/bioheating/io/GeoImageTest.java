package com.greendelta.bioheating.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;

class GeoImageTest {

	@Test
	void usesMaxWidthForWideEnvelopes() {
		try (var image = GeoImage.of(1024, new Envelope(0, 200, 0, 100))) {
			var img = image.getImage();
			assertEquals(1024, img.getWidth());
			assertEquals(512, img.getHeight());
		}
	}

	@Test
	void usesMaxHeightForTallEnvelopes() {
		try (var image = GeoImage.of(1024, new Envelope(0, 100, 0, 200))) {
			var img = image.getImage();
			assertEquals(512, img.getWidth());
			assertEquals(1024, img.getHeight());
		}
	}
}
