package com.greendelta.bioheating.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.BuildingType;
import com.greendelta.bioheating.model.ClimateRegion;
import com.greendelta.bioheating.model.ConstructionAge;
import com.greendelta.bioheating.model.GeoMap;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.model.RoofType;
import com.greendelta.bioheating.model.WarmWater;
import com.greendelta.bioheating.predict.BoostPredictor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;

class XlsBuildingImportTest {

	@TempDir
	Path tempDir;

	private int fileIndex;

	@Test
	void updatesAnExistingBuildingById() throws Exception {
		var project = project();
		var map = project.map();
		var existing = building("b-1", 500000, 5500000, 100)
			.name("old name")
			.type(BuildingType.MULTI_GENERATION)
			.isIncluded(false);
		map.buildings().add(existing);

		var point = toWgs84(map, 500000, 5500000);
		var file = excel(new Object[] {
			"b-1", "new name", point.x, point.y, 6.0, "1994",
			20.0, 400.0, true, 20.0, 15000.0, 7.0,
			"Berlin", "10115", "Main Street", "1"
		});

		var res = new XlsBuildingImport(null, project, file).call();
		assertTrue(res.isOk());

		assertEquals(1, map.buildings().size());
		assertSame(existing, map.buildings().getFirst());
		assertEquals("new name", existing.name());
		assertEquals(BuildingType.SINGLE_FAMILY, existing.type());
		assertEquals(ConstructionAge.AGE_1979_1995, existing.constructionAge());
		assertEquals(20.0, existing.height(), 1e-6);
		assertEquals(400.0, existing.groundArea(), 1e-6);
		assertEquals(RoofType.FLAT, existing.roofType());
		assertEquals(20.0, existing.warmWaterFraction(), 1e-6);
		assertEquals(15000.0, existing.heatDemand(), 1e-6);
		assertEquals(7.0, existing.peakLoad(), 1e-6);
		assertEquals("Berlin", existing.locality());
		assertEquals("10115", existing.postalCode());
		assertEquals("Main Street", existing.street());
		assertEquals("1", existing.streetNumber());
		assertTrue(existing.isHeated());
		assertTrue(existing.isIncluded());
		// the square (400 m2) is larger than the original polygon (100 m2)
		assertEquals(400.0, BuildingIndex.polygonOf(existing).getArea(), 1e-3);
	}

	@Test
	void createsANewBuildingWithAGeneratedId() throws Exception {
		var project = project();
		var map = project.map();
		var point = toWgs84(map, 500000, 5500000);
		var file = excel(new Object[] {
			null, "New Building", point.x, point.y, null, null,
			null, 120.0, null, null, 12000.0, 5.0,
			null, null, "Main Street", "2"
		});

		var res = new XlsBuildingImport(null, project, file).call();
		assertTrue(res.isOk());

		assertEquals(1, map.buildings().size());
		var created = map.buildings().getFirst();
		assertNotNull(created.cityId());
		assertFalse(created.cityId().isBlank());
		assertEquals("New Building", created.name());
		assertTrue(created.isHeated());
		assertTrue(created.isIncluded());
		// area 120 -> low-rise, no neighbors -> single family house
		assertEquals(BuildingType.SINGLE_FAMILY, created.type());
		assertEquals(
			BuildingType.SINGLE_FAMILY.defaultHeight(),
			created.height(),
			1e-6
		);
		assertEquals(120.0, created.groundArea(), 1e-6);
	}

	@Test
	void matchesAnExistingBuildingByGeometry() throws Exception {
		var project = project();
		var map = project.map();
		var existing = building("existing", 500000, 5500000, 400)
			.name("Existing");
		map.buildings().add(existing);

		var point = toWgs84(map, 500000, 5500000);
		// no ID; the square (ground area 100) is inside the existing polygon
		var file = excel(new Object[] {
			null, "Updated", point.x, point.y, null, null,
			null, 100.0, null, null, 10000.0, 4.0,
			null, null, null, null
		});

		var res = new XlsBuildingImport(null, project, file).call();
		assertTrue(res.isOk());

		assertEquals(1, map.buildings().size());
		assertSame(existing, map.buildings().getFirst());
		assertEquals("Updated", existing.name());
		assertEquals("existing", existing.cityId());
		// the square (100 m2) is smaller than the polygon -> keep the polygon
		assertEquals(400.0, BuildingIndex.polygonOf(existing).getArea(), 1e-3);
	}

	@Test
	void keepsExistingValuesAndFillsDefaults() throws Exception {
		var project = project();
		var map = project.map();
		var existing = building("b-1", 500000, 5500000, 100)
			.name("keep")
			.type(BuildingType.MULTI_GENERATION)
			.heatDemand(9000)
			.peakLoad(4);
		map.buildings().add(existing);

		var point = toWgs84(map, 500000, 5500000);
		var file = excel(new Object[] {
			"b-1", null, point.x, point.y, null, null,
			null, null, null, null, null, null,
			null, null, null, null
		});

		var res = new XlsBuildingImport(null, project, file).call();
		assertTrue(res.isOk());

		assertEquals("keep", existing.name());
		assertEquals(BuildingType.MULTI_GENERATION, existing.type());
		assertEquals(
			BuildingType.MULTI_GENERATION.defaultHeight(),
			existing.height(),
			1e-6
		);
		assertEquals(
			BuildingType.MULTI_GENERATION.defaultGroundArea(),
			existing.groundArea(),
			1e-6
		);
		assertEquals(
			WarmWater.of(BuildingType.MULTI_GENERATION, null),
			existing.warmWaterFraction(),
			1e-6
		);
	}

	@Test
	void doesNotOverrideExistingValuesWithNonPositiveOnes() throws Exception {
		var project = project();
		var map = project.map();
		var existing = building("b-1", 500000, 5500000, 300)
			.name("keep")
			.type(BuildingType.SINGLE_FAMILY)
			.height(20)
			.groundArea(300)
			.heatDemand(12000)
			.peakLoad(6);
		map.buildings().add(existing);

		var point = toWgs84(map, 500000, 5500000);
		var file = excel(new Object[] {
			"b-1", null, point.x, point.y, null, null,
			0.0, 0.0, null, null, 0.0, 0.0,
			null, null, null, null
		});

		var res = new XlsBuildingImport(null, project, file).call();
		assertTrue(res.isOk());

		assertEquals(20.0, existing.height(), 1e-6);
		assertEquals(300.0, existing.groundArea(), 1e-6);
		assertEquals(12000.0, existing.heatDemand(), 1e-6);
		assertEquals(6.0, existing.peakLoad(), 1e-6);
	}

	@Test
	void estimatesTheTypeFromTheNumberOfHeatedNeighbors() throws Exception {
		var project = project();
		var map = project.map();
		// a heated building at x: 499990..500010
		map.buildings().add(
			building("existing", 500000, 5500000, 400)
				.isHeated(true)
				.heatDemand(12000)
				.peakLoad(5));

		// 0.1 m to the right of the existing building -> one neighbor
		var point = toWgs84(map, 500015.1, 5500000);
		var file = excel(new Object[] {
			null, "New", point.x, point.y, null, null,
			null, 100.0, null, null, 12000.0, 5.0,
			null, null, null, null
		});

		var res = new XlsBuildingImport(null, project, file).call();
		assertTrue(res.isOk());

		assertEquals(2, map.buildings().size());
		assertEquals(
			BuildingType.END_TERRACE,
			map.buildings().getLast().type()
		);
	}

	@Test
	void requiresAClimateRegionForTheEstimation() throws Exception {
		var project = project(); // no climate region and no database
		var map = project.map();
		var point = toWgs84(map, 500000, 5500000);
		var file = excel(new Object[] {
			null, "New", point.x, point.y, null, null,
			null, 120.0, null, null, null, null,
			null, null, null, null
		});

		var res = new XlsBuildingImport(null, project, file).call();

		assertTrue(res.isError());
		assertTrue(res.error().contains("climate region"));
	}

	@Test
	void estimatesMissingHeatDemandAndPeakLoad() throws Exception {
		assumeTrue(
			BoostPredictor.class.getResource("demand-model.ubj") != null,
			"the prediction model files are not available"
		);

		var project = project();
		project.climateRegion(new ClimateRegion().number(1));
		var map = project.map();
		var point = toWgs84(map, 500000, 5500000);
		var file = excel(new Object[] {
			null, "New", point.x, point.y, null, null,
			null, 120.0, null, null, null, null,
			null, null, null, null
		});

		var res = new XlsBuildingImport(null, project, file).call();
		assertTrue(res.isOk());

		var created = map.buildings().getFirst();
		assertTrue(created.heatDemand() > 0);
		assertTrue(created.peakLoad() > 0);
	}

	private Project project() {
		return new Project().map(new GeoMap().crs("EPSG:25832"));
	}

	private Coordinate toWgs84(GeoMap map, double x, double y) {
		var transformer = CoordinateTransformer.toWgs84From(map);
		assertTrue(transformer.isOk());
		var projected = transformer.value().project(x, y);
		assertTrue(projected.isOk());
		return new Coordinate(projected.value().x, projected.value().y);
	}

	private Building building(
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

	private File excel(Object[]... rows) throws IOException {
		try (var workbook = new XSSFWorkbook()) {
			var sheet = workbook.createSheet();
			sheet.createRow(0); // header row
			for (int r = 0; r < rows.length; r++) {
				var row = sheet.createRow(r + 1);
				var values = rows[r];
				for (int c = 0; c < values.length; c++) {
					var value = values[c];
					if (value == null) continue;
					var cell = row.createCell(c);
					if (value instanceof String text) {
						cell.setCellValue(text);
					} else if (value instanceof Number number) {
						cell.setCellValue(number.doubleValue());
					} else if (value instanceof Boolean flag) {
						cell.setCellValue(flag);
					}
				}
			}
			var file = tempDir
				.resolve("rows-" + (fileIndex++) + ".xlsx")
				.toFile();
			try (var out = new FileOutputStream(file)) {
				workbook.write(out);
			}
			return file;
		}
	}
}
