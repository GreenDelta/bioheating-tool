package com.greendelta.bioheating.citygml;

import static org.junit.jupiter.api.Assertions.*;

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
		assertEquals(1.0, t1.volumeFactor(), 1e-16);

		var t2 = types.get("3100");
		assertNotNull(t2);
		assertEquals("3100", t2.code());
		assertEquals("Satteldach", t2.label());
		assertEquals(0.85, t2.volumeFactor(), 1e-16);

		var t3 = types.get("9999");
		assertNotNull(t3);
		assertEquals("9999", t3.code());
		assertEquals("Sonstiges", t3.label());
		assertEquals(0.85, t3.volumeFactor(), 1e-16);
	}
}
