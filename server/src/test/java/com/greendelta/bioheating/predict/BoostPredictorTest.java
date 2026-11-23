package com.greendelta.bioheating.predict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.BuildingType;
import com.greendelta.bioheating.model.ClimateRegion;
import com.greendelta.bioheating.model.ConstructionAge;
import com.greendelta.bioheating.model.RoofType;
import java.io.File;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BoostPredictorTest {

	@TempDir
	File tempDir;

	@Test
	void appliesTheConfiguredCorrection() throws Exception {
		var models = Training.trainFrom(TestData.writeCsv(tempDir)).orElseThrow();
		var predictor = new BoostPredictor(models.heatDemand(), models.peakLoad());
		var building = new Building()
			.groundArea(100)
			.height(10)
			.type(BuildingType.SINGLE_FAMILY)
			.constructionAge(ConstructionAge.AGE_1979_1995)
			.roofType(RoofType.FLAT);
		var region = new ClimateRegion().number(5);

		var raw = predictor.predict(region, building).orElseThrow();
		assertTrue(raw.heatDemand() > 0);

		var correctedPredictor = new BoostPredictor(
			models.heatDemand(),
			models.peakLoad(),
			new ModelConfig(null, new Correction(2, 1), Correction.NONE)
		);
		var corrected = correctedPredictor.predict(region, building).orElseThrow();
		assertEquals(2 * raw.heatDemand() + 1, corrected.heatDemand(), 1e-3);
		assertEquals(raw.peakLoad(), corrected.peakLoad(), 1e-3);
	}

	@Test
	void loadsTheModelFromADirectory() throws Exception {
		var models = Training.trainFrom(TestData.writeCsv(tempDir)).orElseThrow();
		var dir = new File(tempDir, "model");
		assertTrue(Training.save(models, dir).isOk());
		new ModelConfig("test", new Correction(1.5, 0), Correction.NONE)
			.write(dir);

		var predictor = BoostPredictor.load(dir).orElseThrow();
		assertEquals("test", predictor.config().description());
		assertEquals(new Correction(1.5, 0), predictor.config().heatDemand());
	}

	@Test
	void failsWhenAModelFileIsMissing() throws Exception {
		var models = Training.trainFrom(TestData.writeCsv(tempDir)).orElseThrow();
		var dir = new File(tempDir, "model");
		assertTrue(Training.save(models, dir).isOk());
		assertTrue(new File(dir, Target.PEAK_LOAD.modelFile()).delete());

		var res = BoostPredictor.load(dir);
		assertTrue(res.isError());
		assertTrue(res.error().contains(Target.PEAK_LOAD.modelFile()));
	}

	@Test
	void failsWhenTheConfigIsMissing() throws Exception {
		var models = Training.trainFrom(TestData.writeCsv(tempDir)).orElseThrow();
		var dir = new File(tempDir, "model");
		assertTrue(Training.save(models, dir).isOk());
		assertTrue(new File(dir, ModelConfig.FILE_NAME).delete());

		var res = BoostPredictor.load(dir);
		assertTrue(res.isError());
		assertTrue(res.error().contains(ModelConfig.FILE_NAME));
	}

	@Test
	void failsForAMissingDirectory() {
		var res = BoostPredictor.load(new File(tempDir, "does-not-exist"));
		assertTrue(res.isError());
	}

	@Test
	void predictsASingleBuilding() throws Exception {
		var models = Training.trainFrom(TestData.writeCsv(tempDir)).orElseThrow();
		var predictor = new BoostPredictor(models.heatDemand(), models.peakLoad());
		var building = new Building()
			.groundArea(120)
			.height(9.5)
			.type(BuildingType.MULTI_FAMILY_SMALL)
			.constructionAge(ConstructionAge.AGE_1949_1978)
			.roofType(RoofType.FLAT)
			.isHeated(true);
		var region = new ClimateRegion().number(5);

		var result = predictor.predict(region, building);
		assertFalse(result.isError());
		assertTrue(result.value().heatDemand() > 0);
		assertTrue(result.value().peakLoad() > 0);
	}
}
