package com.greendelta.bioheating.citygml;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.greendelta.bioheating.Tests;
import com.greendelta.bioheating.io.CityGmlImport;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Project;

public class CityGmlImportTest {

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
		var project = new Project().name("test project");
		var res = new CityGmlImport(db, project, file).call();
		assertFalse(res.hasError());
		project = res.value();

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
		assertEquals("31001_1010", building.function());
		assertEquals("3100", building.roofType());
		assertEquals(10.354, building.height(), 1e-3);
		assertEquals(1, building.storeys());

		// check address data
		assertEquals("Germany", building.country());
		assertEquals("Hamburg", building.locality());
		assertEquals("Ohlenkamp", building.street());
		assertEquals("8b", building.streetNumber());
		assertEquals("22607", building.postalCode());
		// check calculated areas and volume should be > 0
		assertTrue(building.groundArea() > 0);
		assertTrue(building.heatedArea() > 0);
		assertTrue(building.volume() > 0);

		// check heat demand prediction is calculated
		assertTrue(building.heatDemand() >= 0);

		db.delete(project);
	}
}
