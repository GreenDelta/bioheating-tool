package com.greendelta.bioheating.citygml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.greendelta.bioheating.Tests;
import com.greendelta.bioheating.io.citygml.CityGmlImport;
import com.greendelta.bioheating.model.ClimateRegion;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Project;

public class CityGmlImportTest {

	private static final boolean WITH_OSM = false;
	private final Database db = Tests.db();
	private File file;

	@BeforeEach
	public void setup() throws IOException {
		var temp = Files.createTempFile("example", ".xml");
		try (var stream = getClass().getResourceAsStream("example.xml")) {
			Objects.requireNonNull(stream);
			Files.copy(stream, temp, StandardCopyOption.REPLACE_EXISTING);
		}
		file = temp.toFile();
	}

	@AfterEach
	public void cleanup() throws IOException {
		Files.delete(file.toPath());
	}

	@Test
	public void testImport() {
		var region = db.insert(new ClimateRegion().number(13));
		var project = new Project().name("test project")
			.climateRegion(region);
		project = new CityGmlImport(db, project, file)
			.withOsmImport(WITH_OSM)
			.call()
			.orElseThrow();

		var map = project.map();
		assertNotNull(map);
		assertEquals("EPSG:25832", map.crs());
		assertEquals(1, map.buildings().size());

		// check the building
		var building = map.buildings().getFirst();
		assertEquals("Ohlenkamp 8b", building.name());
		var cs = building.coordinates();
		assertNotNull(cs);
		assertEquals(5, cs.length);
		assertEquals("31001_1010", building.functionCode());
		assertEquals("3100", building.roofTypeCode());
		assertEquals(10.354, building.height(), 1e-3);
		assertEquals(1, building.storeys());

		// check address data
		assertEquals("Germany", building.country());
		assertEquals("Hamburg", building.locality());
		assertEquals("Ohlenkamp", building.street());
		assertEquals("8b", building.streetNumber());
		assertEquals("22607", building.postalCode());
		assertTrue(building.groundArea() > 0);

		// check heat demand prediction is calculated
		assertTrue(building.heatDemand() > 0);

		if (WITH_OSM) {
			assertFalse(map.streets().isEmpty());
		}

		db.delete(project);
		db.delete(region);
	}
}
