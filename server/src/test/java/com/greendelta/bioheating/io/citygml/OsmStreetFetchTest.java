package com.greendelta.bioheating.io.citygml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.greendelta.bioheating.model.GeoMap;
import com.greendelta.bioheating.model.Street;
import java.util.List;
import org.junit.jupiter.api.Test;

class OsmStreetFetchTest {

	@Test
	void addsOnlyStreetsWithNewOsmIds() {
		var map = new GeoMap();
		var existing = new Street().osmId(1).name("existing").isExcluded(true);
		map.streets().add(existing);

		int added = OsmStreetFetch.addNew(map, List.of(
			new Street().osmId(1).name("fetched again"),
			new Street().osmId(2).name("new"),
			new Street().osmId(2).name("new duplicate")
		));

		assertEquals(1, added);
		assertEquals(2, map.streets().size());
		assertEquals("existing", map.streets().get(0).name());
		assertTrue(map.streets().get(0).isExcluded());
		assertEquals("new", map.streets().get(1).name());
	}
}
