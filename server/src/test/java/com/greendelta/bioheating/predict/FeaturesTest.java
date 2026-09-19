package com.greendelta.bioheating.predict;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.BuildingType;
import com.greendelta.bioheating.model.ConstructionAge;
import com.greendelta.bioheating.model.RoofType;
import java.io.File;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FeaturesTest {

	@TempDir
	File tempDir;

	@Test
	void readsCsvColumnsByIndexAndIgnoresHeaders() throws Exception {
		var csv = new File(tempDir, "data.csv");
		Files.writeString(
			csv.toPath(),
			"a,b,c,d,e,f,g,h\n40,3,1,4,1,6,5583.6,2873.1\n"
		);

		var item = CsvItem.readFrom(csv).orElseThrow().get(0);
		assertEquals(40.0, item.groundArea());
		assertEquals(3.0, item.height());
		assertEquals(1, item.weatherStation());
		assertEquals(4, item.constructionYear());
		assertEquals(1, item.roofType());
		assertEquals(6, item.buildingType());
		assertEquals(5583.6, item.heatDemand());
		assertEquals(2873.1, item.peakLoad());
	}

	@Test
	void encodesBuildingFeaturesInModelOrder() {
		var building = new Building()
			.groundArea(100)
			.height(10)
			.type(BuildingType.SINGLE_FAMILY)
			.constructionAge(ConstructionAge.AGE_1979_1995)
			.roofType(RoofType.FLAT);

		var data = new float[Features.COUNT];
		Features.of(7, building, data, 0);
		assertArrayEquals(new float[] { 100f, 10f, 7f, 4f, 1f, 6f }, data);
	}

	@Test
	void defaultsMissingConstructionAgeAndPitchedRoof() {
		var building = new Building()
			.groundArea(100)
			.height(10)
			.type(BuildingType.SINGLE_FAMILY)
			.roofType(RoofType.PITCHED);

		var data = new float[Features.COUNT];
		Features.of(7, building, data, 0);
		assertArrayEquals(new float[] { 100f, 10f, 7f, 4f, 0f, 6f }, data);
	}

	@Test
	void rejectsBuildingsWithoutValidType() {
		var building = new Building().type(BuildingType.OTHER);
		var data = new float[Features.COUNT];
		assertThrows(
			IllegalArgumentException.class,
			() -> Features.of(7, building, data, 0)
		);
	}
}
