package com.greendelta.bioheating.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.greendelta.bioheating.io.citygml.CityGmlImport;
import com.greendelta.bioheating.model.BuildingType;
import com.greendelta.bioheating.model.ClimateRegion;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.predict.BoostPredictor;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class ProjectCreatorTest {

	private final List<File> tempFiles = new ArrayList<>();

	@AfterEach
	public void cleanup() throws IOException {
		for (var file : tempFiles) {
			Files.deleteIfExists(file.toPath());
		}
	}

	@Test
	public void testUnsupportedFilesAreSkipped() {
		var creator = new ProjectCreator(
			null,
			new Project().name("test project"),
			List.of(tempFile("notes.txt"), tempFile("data.csv")));

		var res = creator.importFiles();

		// unsupported files must not be handed to an importer; when nothing
		// importable is left, the import fails with a clear error
		assertTrue(res.isError());
		assertEquals("No import files provided", res.error());
	}

	@Test
	public void testNoFilesProvided() {
		var creator = new ProjectCreator(
			null, new Project().name("test project"), List.of());

		var res = creator.importFiles();

		assertTrue(res.isError());
		assertEquals("No import files provided", res.error());
	}

	@Test
	public void testCityGmlFilesAreImportedBeforeExcel() {
		var gml = tempFile("model.gml");
		var zippedGml = tempFile("from-zip.gml");
		var excel = tempFile("buildings.xlsx");
		var zippedExcel = tempFile("from-zip.xlsx");
		var other = tempFile("notes.txt");

		var ordered = ProjectCreator.orderedFiles(
			List.of(excel, gml, other, zippedExcel, zippedGml));

		// CityGML first (also from ZIP archives), then Excel; others skipped
		assertEquals(List.of(gml, zippedGml, excel, zippedExcel), ordered);
	}

	@Test
	public void testExcelUpdatesTheImportedCityGmlBuilding() throws IOException {
		assumeTrue(
			BoostPredictor.class.getResource("demand-model.ubj") != null,
			"the prediction model files are not available"
		);

		var gml = tempFile("example.xml");
		try (
			var stream = getClass().getResourceAsStream(
				"/com/greendelta/bioheating/citygml/example.xml")
		) {
			Objects.requireNonNull(stream);
			Files.copy(stream, gml.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}

		// find the ID and position of the GML building with a probe project
		var region = new ClimateRegion().number(13);
		var probe = new Project().name("probe").climateRegion(region);
		var probeRes = new CityGmlImport(null, probe, List.of(gml)).call();
		assertTrue(probeRes.isOk());
		var gmlBuilding = probe.map().buildings().getFirst();
		var id = gmlBuilding.cityId();
		var transformer = CoordinateTransformer
			.toWgs84From(probe.map())
			.orElseThrow();
		var center = BuildingIndex
			.polygonOf(gmlBuilding)
			.getCentroid()
			.getCoordinate();
		var point = transformer.project(center.x, center.y).orElseThrow();

		// the Excel row updates that building; note the reverse file order
		var excel = tempFile("buildings.xlsx");
		writeWorkbook(excel, new Object[] {
			id, "Updated from Excel", point.x, point.y, 6.0, "1994",
			12.0, 100.0, false, 15.0, 20000.0, 9.0,
			"Hamburg", "22607", "Ohlenkamp", "8b"
		});

		var project = new Project().name("test").climateRegion(region);
		var res = new ProjectCreator(null, project, List.of(excel, gml))
			.importFiles();
		assertTrue(res.isOk());

		var map = project.map();
		assertNotNull(map);
		assertEquals(1, map.buildings().size());
		var building = map.buildings().getFirst();
		assertEquals(id, building.cityId());
		assertEquals("Updated from Excel", building.name());
		assertEquals(BuildingType.SINGLE_FAMILY, building.type());
		assertEquals(12.0, building.height(), 1e-6);
		assertEquals(20000.0, building.heatDemand(), 1e-6);
		assertEquals(9.0, building.peakLoad(), 1e-6);
	}

	private void writeWorkbook(File file, Object[] row) throws IOException {
		try (var workbook = new XSSFWorkbook()) {
			var sheet = workbook.createSheet();
			sheet.createRow(0); // header row
			var data = sheet.createRow(1);
			for (int c = 0; c < row.length; c++) {
				var value = row[c];
				if (value == null) continue;
				var cell = data.createCell(c);
				if (value instanceof String text) {
					cell.setCellValue(text);
				} else if (value instanceof Number number) {
					cell.setCellValue(number.doubleValue());
				} else if (value instanceof Boolean flag) {
					cell.setCellValue(flag);
				}
			}
			try (var out = Files.newOutputStream(file.toPath())) {
				workbook.write(out);
			}
		}
	}

	private File tempFile(String name) {
		try {
			var path = Files.createTempFile("import-", "-" + name);
			tempFiles.add(path.toFile());
			return path.toFile();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
