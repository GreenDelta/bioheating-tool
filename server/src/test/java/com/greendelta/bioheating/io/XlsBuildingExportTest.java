package com.greendelta.bioheating.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.BuildingType;
import com.greendelta.bioheating.model.ConstructionAge;
import com.greendelta.bioheating.model.GeoMap;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.model.RoofType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;

class XlsBuildingExportTest {

	@TempDir
	Path tempDir;

	@Test
	void roundTripsThroughTheExcelFormat() throws Exception {
		var source = project();
		source.map().buildings().add(
			squareBuilding("b-1", 500000, 5500000, 400)
				.name("Main Street 1")
				.type(BuildingType.SINGLE_FAMILY)
				.constructionAge(ConstructionAge.AGE_1995_2009)
				.height(20)
				.groundArea(400)
				.roofType(RoofType.FLAT)
				.warmWaterFraction(17.5)
				.heatDemand(15000)
				.peakLoad(7)
				.locality("Berlin")
				.postalCode("10115")
				.street("Main Street")
				.streetNumber("1")
				.isHeated(true)
				.isIncluded(true)
		);

		var file = export(source);

		var target = project();
		var res = new XlsBuildingImport(null, target, file).call();
		assertTrue(res.isOk());
		assertEquals(1, target.map().buildings().size());

		var imported = target.map().buildings().getFirst();
		assertEquals("b-1", imported.cityId());
		assertEquals("Main Street 1", imported.name());
		assertEquals(BuildingType.SINGLE_FAMILY, imported.type());
		assertEquals(ConstructionAge.AGE_1995_2009, imported.constructionAge());
		assertEquals(20.0, imported.height(), 1e-6);
		assertEquals(400.0, imported.groundArea(), 1e-6);
		assertEquals(RoofType.FLAT, imported.roofType());
		assertEquals(17.5, imported.warmWaterFraction(), 1e-6);
		assertEquals(15000.0, imported.heatDemand(), 1e-6);
		assertEquals(7.0, imported.peakLoad(), 1e-6);
		assertEquals("Berlin", imported.locality());
		assertEquals("10115", imported.postalCode());
		assertEquals("Main Street", imported.street());
		assertEquals("1", imported.streetNumber());
		assertTrue(imported.isHeated());
		assertTrue(imported.isIncluded());
		assertEquals(400.0, BuildingIndex.polygonOf(imported).getArea(), 1e-3);
	}

	@Test
	void writesTheHeaderInTheDocumentedOrder() throws Exception {
		var file = export(project());

		try (
			var in = new ByteArrayInputStream(Files.readAllBytes(file.toPath()));
			var workbook = new XSSFWorkbook(in)
		) {
			var row = workbook.getSheetAt(0).getRow(0);
			var headers = new ArrayList<String>();
			for (int i = 0; i < 16; i++) {
				headers.add(row.getCell(i).getStringCellValue());
			}
			assertEquals(
				List.of(
					"id", "name", "longitude", "latitude", "building type",
					"construction year", "height", "ground area", "flat roof",
					"warm water fraction", "heat demand", "peak load",
					"city", "postal code", "street", "number"
				),
				headers
			);
		}
	}

	private File export(Project project) throws Exception {
		try (var out = new ByteArrayOutputStream()) {
			var res = new XlsBuildingExport(project, out).run();
			assertTrue(res.isOk());
			var file = tempDir.resolve("export.xlsx").toFile();
			Files.write(file.toPath(), out.toByteArray());
			return file;
		}
	}

	private static Project project() {
		return new Project().map(new GeoMap().crs("EPSG:25832"));
	}

	private static Building squareBuilding(
		String cityId, double centerX, double centerY, double area
	) {
		double d = Math.sqrt(area) / 2;
		return new Building().cityId(cityId).coordinates(new Coordinate[] {
			new Coordinate(centerX - d, centerY - d),
			new Coordinate(centerX + d, centerY - d),
			new Coordinate(centerX + d, centerY + d),
			new Coordinate(centerX - d, centerY + d),
			new Coordinate(centerX - d, centerY - d),
		});
	}
}
