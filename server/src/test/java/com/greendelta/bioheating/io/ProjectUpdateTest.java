package com.greendelta.bioheating.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.greendelta.bioheating.TestModel;
import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.BuildingType;
import com.greendelta.bioheating.model.ClimateRegion;
import com.greendelta.bioheating.model.ConstructionAge;
import com.greendelta.bioheating.model.GeoMap;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.model.RoofType;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;

class ProjectUpdateTest {

	@TempDir
	Path tempDir;

	@Test
	void rejectsAnUpdateWithoutSteps() {
		var res = new ProjectUpdate(null, project(), null).call();
		assertTrue(res.isError());
	}

	@Test
	void estimatesAllHeatedBuildings() {
		assumeTrue(TestModel.isAvailable(), TestModel.MISSING);
		var project = project();
		var heated = building("heated", 500000, 5500000).isHeated(true);
		var unheated = building("unheated", 500100, 5500000)
			.isHeated(false)
			.heatDemand(0)
			.peakLoad(0);
		project.map().buildings().add(heated);
		project.map().buildings().add(unheated);

		var res = new ProjectUpdate(null, project, TestModel.get())
			.withEstimation(true)
			.update();

		assertTrue(res.isOk(), () -> res.error());
		assertNotEquals(1.0, heated.heatDemand());
		assertNotEquals(1.0, heated.peakLoad());
		assertEquals(0.0, unheated.heatDemand());
		assertEquals(0.0, unheated.peakLoad());
	}

	@Test
	void keepsTheValuesOfTheExcelFile() throws Exception {
		assumeTrue(TestModel.isAvailable(), TestModel.MISSING);
		var project = project();
		var inFile = building("in-file", 500000, 5500000).isHeated(true);
		var other = building("other", 500100, 5500000).isHeated(true);
		project.map().buildings().add(inFile);
		project.map().buildings().add(other);

		// the file provides only the heat demand of the first building
		var point = toWgs84(project.map(), 500000, 5500000);
		var file = excel(new Object[] {
			"in-file", null, point.x, point.y, null, null,
			null, null, null, null, 15000.0, null,
		});

		var res = new ProjectUpdate(null, project, TestModel.get())
			.withExcelFile(file)
			.withEstimation(true)
			.update();

		assertTrue(res.isOk(), () -> res.error());
		assertEquals(15000.0, inFile.heatDemand());
		assertNotEquals(1.0, inFile.peakLoad());
		assertNotEquals(1.0, other.heatDemand());
		assertNotEquals(1.0, other.peakLoad());
	}

	private Project project() {
		return new Project()
			.climateRegion(new ClimateRegion().number(5))
			.map(new GeoMap().crs("EPSG:25832"));
	}

	/// A building with all attributes that are needed for an estimation and the
	/// marker value 1 as heat demand and peak load.
	private Building building(String cityId, double x, double y) {
		double d = 5;
		return new Building()
			.cityId(cityId)
			.groundArea(100)
			.height(9)
			.type(BuildingType.MULTI_FAMILY_SMALL)
			.constructionAge(ConstructionAge.AGE_1949_1978)
			.roofType(RoofType.FLAT)
			.isIncluded(true)
			.heatDemand(1)
			.peakLoad(1)
			.coordinates(new Coordinate[] {
				new Coordinate(x - d, y - d),
				new Coordinate(x + d, y - d),
				new Coordinate(x + d, y + d),
				new Coordinate(x - d, y + d),
				new Coordinate(x - d, y - d),
			});
	}

	private Coordinate toWgs84(GeoMap map, double x, double y) {
		var transformer = CoordinateTransformer.toWgs84From(map).orElseThrow();
		var projected = transformer.project(x, y).orElseThrow();
		return new Coordinate(projected.x, projected.y);
	}

	private File excel(Object[] row) throws Exception {
		try (var workbook = new XSSFWorkbook()) {
			var sheet = workbook.createSheet();
			sheet.createRow(0); // header row
			var r = sheet.createRow(1);
			for (int c = 0; c < row.length; c++) {
				var value = row[c];
				if (value == null) continue;
				var cell = r.createCell(c);
				if (value instanceof String text) {
					cell.setCellValue(text);
				} else if (value instanceof Number number) {
					cell.setCellValue(number.doubleValue());
				}
			}
			var file = tempDir.resolve("update.xlsx").toFile();
			try (var out = new FileOutputStream(file)) {
				workbook.write(out);
			}
			return file;
		}
	}
}
