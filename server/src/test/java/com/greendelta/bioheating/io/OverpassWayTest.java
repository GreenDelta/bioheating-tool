package com.greendelta.bioheating.io;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class OverpassWayTest {

	private OverpassWay way;

	@BeforeEach
	public void setup() throws Exception {
		var stream = getClass().getResourceAsStream("overpass-way.json");
		assertNotNull(stream);
		try (stream) {
			var obj = new ObjectMapper()
				.createParser(stream)
				.readValueAs(ObjectNode.class);
			way = new OverpassWay(obj);
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
	public void testCoordinates() {
		var coords = way.geometry();
		assertNotNull(coords);
		assertEquals(4, coords.size());

		// check first coordinate
		assertEquals(11.4879310, coords.get(0).x, 0.0000001);
		assertEquals(48.8306223, coords.get(0).y, 0.0000001);

		// check last coordinate
		assertEquals(11.4876697, coords.get(3).x, 0.0000001);
		assertEquals(48.8304665, coords.get(3).y, 0.0000001);
	}

	@Test
	public void testOtherData() {
		assertEquals("way", way.type());
		assertEquals(28419583, way.id());
	}

}
