package com.greendelta.bioheating.io;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class OsmStreetTest {

	private OsmStreet way;

	@BeforeEach
	public void setup() throws Exception {
		var stream = getClass().getResourceAsStream("osm-street.json");
		assertNotNull(stream);
		try (stream) {
			way = new ObjectMapper().createParser(stream)
				.readValueAs(OsmStreet.class);
		}
	}

	@Test
	public void testTags() {
		var tags = way.tags();
		assertNotNull(tags);
		assertEquals(3, tags.size());
		assertEquals("unclassified", tags.get("highway"));
		assertEquals("Am Dürrnhof", tags.get("name"));
		assertEquals("asphalt", tags.get("surface"));
	}

	@Test
	public void testBounds() {
		var bounds = way.bounds();
		assertNotNull(bounds);
		assertEquals(48.8304665, bounds.minlat(), 0.0000001);
		assertEquals(11.4876697, bounds.minlon(), 0.0000001);
		assertEquals(48.8306223, bounds.maxlat(), 0.0000001);
		assertEquals(11.4879310, bounds.maxlon(), 0.0000001);
	}

	@Test
	public void testNodes() {
		var nodes = way.nodes();
		assertNotNull(nodes);
		assertEquals(4, nodes.size());
		assertEquals(1505676564L, nodes.get(0));
		assertEquals(1505676555L, nodes.get(3));
	}

	@Test
	public void testGeometry() {
		var geometry = way.geometry();
		assertNotNull(geometry);
		assertEquals(4, geometry.size());

		// check first coordinate
		assertEquals(48.8306223, geometry.get(0).lat(), 0.0000001);
		assertEquals(11.4879310, geometry.get(0).lon(), 0.0000001);

		// check last coordinate
		assertEquals(48.8304665, geometry.get(3).lat(), 0.0000001);
		assertEquals(11.4876697, geometry.get(3).lon(), 0.0000001);
	}

	@Test
	public void testOtherData() {
		assertEquals("way", way.type());
		assertEquals(28419583, way.id());
	}

}
