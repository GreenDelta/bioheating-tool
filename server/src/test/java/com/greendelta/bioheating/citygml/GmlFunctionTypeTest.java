package com.greendelta.bioheating.citygml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class GmlFunctionTypeTest {

	@Test
	public void testGetAll() {
		var types = GmlFunctionType.getAll();
		assertEquals(301, types.size());

		var t1 = types.get("31001_1010");
		assertNotNull(t1);
		assertEquals("31001_1010", t1.code());
		assertEquals("Wohnhaus", t1.label());
		assertTrue(t1.isHeated());

		var t2 = types.get("31001_1313");
		assertNotNull(t2);
		assertEquals("31001_1313", t2.code());
		assertEquals("Gartenhaus", t2.label());
		assertFalse(t2.isHeated());

		var t3 = types.get("31001_2111");
		assertNotNull(t3);
		assertEquals("31001_2111", t3.code());
		assertEquals("Fabrik", t3.label());
		assertFalse(t3.isHeated());
	}

}
