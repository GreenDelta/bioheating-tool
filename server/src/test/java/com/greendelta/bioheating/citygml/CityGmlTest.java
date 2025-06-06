package com.greendelta.bioheating.citygml;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

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

	@Test
	public void testBuildingParameters() {
		var b = model.buildings().getFirst();
		assertEquals("DEHH_fe358f01-2ce4-4487-a47b-8989435fb552", b.id());
		assertEquals("31001_1010", b.function());
		assertEquals(10.354, b.height(), 1e-3);
		assertEquals(1, b.storeys());
	}

	@Test
	public void testBuildingAddress() {
		var a = model.buildings().getFirst().address();
		assertEquals("Germany", a.country());
		assertEquals("Hamburg", a.locality());
		assertEquals("Ohlenkamp", a.street());
		assertEquals("8", a.number());
		// assertEquals("8b", ...
		// assertEquals("22607", a.postalCode());
	}

	@Test
	public void testBuildingAttributes() {
		var m = model.buildings().getFirst().attributes();
		assertEquals("02000000", m.get("Gemeindeschluessel"));
		assertEquals("1000", m.get("DatenquelleDachhoehe"));
		assertEquals("1000", m.get("DatenquelleLage"));
		assertEquals("1300", m.get("DatenquelleBodenhoehe"));
	}

	@Test
	public void testBuildingPolygon() {
		var poly = model.buildings().getFirst().groundSurface();
		var expectedCoords = new Coordinate[]{
			new Coordinate(558459.101, 5935606.158, 28.598),
			new Coordinate(558466.79, 5935606.053, 28.598),
			new Coordinate(558466.67, 5935597.228, 28.598),
			new Coordinate(558459.125, 5935597.332, 28.598),
			new Coordinate(558459.101, 5935606.158, 28.598)
		};
		var coords = poly.getCoordinates();
		assertEquals(expectedCoords.length, coords.length);
		for (int i = 0; i < expectedCoords.length; i++) {
			var e = expectedCoords[i];
			var a = coords[i];
			assertEquals(e.x, a.x, 1e-3);
			assertEquals(e.y, a.y, 1e-3);
			assertEquals(e.z, a.z, 1e-3);
		}
	}
}
