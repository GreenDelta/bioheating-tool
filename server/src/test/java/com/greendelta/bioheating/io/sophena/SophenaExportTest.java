package com.greendelta.bioheating.io.sophena;

import static org.junit.jupiter.api.Assertions.*;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.BuildingType;
import com.greendelta.bioheating.model.GeoMap;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.model.Solution;
import java.io.File;
import java.io.FileInputStream;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;
import tools.jackson.databind.json.JsonMapper;

public class SophenaExportTest {

	@Test
	public void testExportHeatingLimit(@TempDir File tempDir) throws Exception {
		var map = new GeoMap();
		map.crs("EPSG:4326");

		// Building 1: Single family house default
		var b1 = new Building()
			.cityId("b1")
			.name("Building 1")
			.isHeated(true)
			.isIncluded(true)
			.type(BuildingType.SINGLE_FAMILY)
			.coordinates(new Coordinate[] {
				new Coordinate(13.4, 52.5),
				new Coordinate(13.41, 52.5),
				new Coordinate(13.41, 52.51),
				new Coordinate(13.4, 52.5)
			});

		// Building 2: Best match for KfW 40 (loadHours around 928 -> heatingLimit 10)
		var b2 = new Building()
			.cityId("b2")
			.name("Building 2")
			.isHeated(true)
			.isIncluded(true)
			.type(BuildingType.SINGLE_FAMILY)
			.heatDemand(9280)
			.peakLoad(10) // loadHours = 928
			.coordinates(new Coordinate[] {
				new Coordinate(13.4, 52.5),
				new Coordinate(13.41, 52.5),
				new Coordinate(13.41, 52.51),
				new Coordinate(13.4, 52.5)
			});

		map.buildings().add(b1);
		map.buildings().add(b2);

		var project = new Project().map(map);
		var solution = new Solution().project(project);

		var file = new File(tempDir, "export.json.gz");
		var res = SophenaExport.write(solution, file);
		assertTrue(res.isOk());
		assertTrue(file.exists());

		var mapper = JsonMapper.builder().build();
		try (
			var fis = new FileInputStream(file);
			var gzis = new GZIPInputStream(fis)
		) {
			var jsonNode = mapper.readTree(gzis);
			var consumers = jsonNode.get("consumers");
			assertNotNull(consumers);
			assertEquals(2, consumers.size());

			// b1 has default single family house -> Standard 1979-1994 -> heatingLimit 15.0
			var c1 = consumers.get(0);
			assertEquals("Building 1", c1.get("name").asString());
			assertEquals(15.0, c1.get("heatingLimit").asDouble(), 1e-6);

			// b2 has KfW 40 single family house -> heatingLimit 10.0
			var c2 = consumers.get(1);
			assertEquals("Building 2", c2.get("name").asString());
			assertEquals(10.0, c2.get("heatingLimit").asDouble(), 1e-6);
		}
	}
}
