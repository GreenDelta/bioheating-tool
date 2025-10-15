package com.greendelta.bioheating.citygml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class GmlRoofTypeTest {

	@Test
	public void testGetAll() {
		var types = GmlRoofType.getAll();
		assertEquals(15, types.size());

		var t1 = types.get("1000");
		assertNotNull(t1);
		assertEquals("1000", t1.code());
		assertEquals("Flachdach", t1.label());

		var t2 = types.get("3100");
		assertNotNull(t2);
		assertEquals("3100", t2.code());
		assertEquals("Satteldach", t2.label());

		var t3 = types.get("9999");
		assertNotNull(t3);
		assertEquals("9999", t3.code());
		assertEquals("Sonstiges", t3.label());
	}

}
