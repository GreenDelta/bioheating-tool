package com.greendelta.bioheating.model.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.GeoMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

public class MapSyncTest {

	@Test
	public void createsNewBuildingsFromClientFeatures() {
		var map = new GeoMap().crs("EPSG:4326");

		var clientMap = new ClientMap(List.of(new GeoFeature(
			"Feature",
			Geometry.polygonOf(new Coordinate[]{
				new Coordinate(10.0, 50.0),
				new Coordinate(10.001, 50.0),
				new Coordinate(10.001, 50.001),
				new Coordinate(10.0, 50.0),
			}),
			Map.ofEntries(
				Map.entry("@type", "building"),
				Map.entry("id", -1),
				Map.entry("name", "Drawn building"),
				Map.entry("height", 12.5),
				Map.entry("storeys", 3),
				Map.entry("groundArea", 85.0),
				Map.entry("heatDemand", 14000.0),
				Map.entry("peakLoad", 32.0),
				Map.entry("isHeated", true),
				Map.entry("isSupplyCenter", false),
				Map.entry("isIncluded", true),
				Map.entry("type", "OTHER"),
				Map.entry("constructionAge", "UNKNOWN")
			)
		)));

		MapSync.updateFromClient(map, clientMap);

		assertEquals(1, map.buildings().size());
		var created = map.buildings().getFirst();
		assertEquals("Drawn building", created.name());
		assertEquals("client:-1", created.cityId());
		assertEquals(12.5, created.height(), 1e-6);
		assertEquals(3, created.storeys());
		assertEquals(85.0, created.groundArea(), 1e-6);
		assertEquals(14000.0, created.heatDemand(), 1e-6);
		assertEquals(32.0, created.peakLoad(), 1e-6);
		assertTrue(created.isHeated());
		assertTrue(created.isIncluded());
		assertFalse(created.isSupplyCenter());
		assertNotNull(created.coordinates());
		assertEquals(4, created.coordinates().length);
	}

	@Test
	public void removesBuildingsMissingFromClientMap() {
		var map = new GeoMap().crs("EPSG:4326");
		var retained = new Building()
			.name("Retained")
			.coordinates(square())
			.isHeated(true)
			.isIncluded(true);
		retained.id(1);
		var deleted = new Building()
			.name("Deleted")
			.coordinates(square())
			.isHeated(true)
			.isIncluded(true);
		deleted.id(2);
		map.buildings().add(retained);
		map.buildings().add(deleted);

		var clientMap = new ClientMap(List.of(new GeoFeature(
			"Feature",
			Geometry.polygonOf(square()),
			Map.of(
				"@type", "building",
				"id", 1,
				"name", "Retained"
			)
		)));

		MapSync.updateFromClient(map, clientMap);

		assertEquals(1, map.buildings().size());
		assertEquals(1, map.buildings().getFirst().id());
		assertEquals("Retained", map.buildings().getFirst().name());
	}

	private Coordinate[] square() {
		return new Coordinate[] {
			new Coordinate(10.0, 50.0),
			new Coordinate(10.001, 50.0),
			new Coordinate(10.001, 50.001),
			new Coordinate(10.0, 50.0),
		};
	}
}
