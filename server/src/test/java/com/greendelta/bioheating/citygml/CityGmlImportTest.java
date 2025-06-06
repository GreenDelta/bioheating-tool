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
		assertEquals("urn:adv:crs:ETRS89_UTM32*DE_DHHN92_NH", map.crs());
		assertEquals(1, map.buildings().size());

		// check the building
		var building = map.buildings().getFirst();
		assertEquals("Ohlenkamp 8b", building.name());
		var cs = building.coordinates();
		assertNotNull(cs);
		assertEquals(5, cs.length);

		db.delete(project);
	}
}
