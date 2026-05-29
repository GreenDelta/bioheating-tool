package com.greendelta.bioheating.model.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.greendelta.bioheating.model.Building;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PropertyPatchTest {

	@Test
	void normalizesSupplyCenterFlags() {
		var building = new Building()
			.isHeated(true)
			.isIncluded(true)
			.isSupplyCenter(false);

		var patch = PropertyPatch.of(new GeoFeature(
			"Feature",
			null,
			Map.of(
				"isHeated", true,
				"isIncluded", true,
				"isSupplyCenter", true
			)
		));

		patch.applyOn(building);

		assertTrue(building.isSupplyCenter());
		assertFalse(building.isHeated());
		assertFalse(building.isIncluded());
	}
}
