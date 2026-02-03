package com.greendelta.bioheating.io.sophena;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class SophenaBuildingStateTest {

	@Test
	public void testGetAll() {
		var states = SophenaBuildingState.getAll();
		assertNotNull(states);
		assertFalse(states.isEmpty());
		assertEquals(63, states.size());

		var first = states.getFirst();
		assertEquals("063b5c74-3c41-4a6a-861c-520c7ad57a71", first.id());
		assertEquals("Niedrigenergiehaus (KfW 70)", first.name());
		assertEquals(SophenaBuildingType.MULTI_FAMILY_HOUSE, first.buildingType());
		assertEquals(1261.0, first.loadHours(), 1e-6);
		assertFalse(first.isDefault());

		var some = states.get(15);
		assertEquals("336f1a57-637e-4fc5-bf34-49a0cfcbab39", some.id());
		assertEquals("Standard", some.name());
		assertEquals(SophenaBuildingType.SCHOOL, some.buildingType());
		assertEquals(1400.0, some.loadHours(), 1e-6);
		assertTrue(some.isDefault());
	}
}
