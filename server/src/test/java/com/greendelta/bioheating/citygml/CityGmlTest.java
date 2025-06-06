package com.greendelta.bioheating.citygml;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CityGmlTest {

	private static GmlModel model;

	@BeforeEach
	public void setup() throws Exception {
		try (var stream = getClass().getResourceAsStream("example.xml")) {
			model = GmlModel.readFrom(stream).orElseThrow();
		}
	}

	@Test
	public void testReadEnvelope() {

		var env = model.envelope();
		assertEquals("urn:adv:crs:ETRS89_UTM32*DE_DHHN92_NH", env.srs());
		assertEquals(3, env.dimension());

		var lower = env.lowerCorner();
		assertNotNull(lower);
		assertEquals(557991.826, lower.getX(), 0.0001);
		assertEquals(5934991.706, lower.getY(), 0.0001);
		assertEquals(20.954, lower.getCoordinate().getZ(), 0.0001);

		var upper = env.upperCorner();
		assertNotNull(upper, "Upper corner should not be null");
		assertEquals(559007.874, upper.getX(), 0.0001);
		assertEquals(5936004.045, upper.getY(), 0.0001);
		assertEquals(54.363, upper.getCoordinate().getZ(), 0.0001);
	}

}
