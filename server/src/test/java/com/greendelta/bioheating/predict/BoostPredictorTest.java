package com.greendelta.bioheating.predict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.BuildingType;
import com.greendelta.bioheating.model.ClimateRegion;
import com.greendelta.bioheating.model.ConstructionAge;
import com.greendelta.bioheating.model.RoofType;
import java.io.File;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BoostPredictorTest {

	@TempDir
	File tempDir;

	@AfterEach
	void resetCorrections() {
		ModelCorrection.configure(Correction.NONE, Correction.NONE);
	}

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

		ModelCorrection.configure(new Correction(2, 1), Correction.NONE);
		var corrected = predictor.predict(region, building).orElseThrow();
		assertEquals(2 * raw.heatDemand() + 1, corrected.heatDemand(), 1e-3);
		assertEquals(raw.peakLoad(), corrected.peakLoad(), 1e-3);
	}
}
