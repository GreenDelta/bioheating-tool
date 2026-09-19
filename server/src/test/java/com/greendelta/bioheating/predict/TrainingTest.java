package com.greendelta.bioheating.predict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.BuildingType;
import com.greendelta.bioheating.model.ClimateRegion;
import com.greendelta.bioheating.model.ConstructionAge;
import java.io.File;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TrainingTest {

	@TempDir
	File tempDir;

	@Test
	void trainsSavesAndAppliesBothModels() throws Exception {
		var csv = TestData.writeCsv(tempDir);

		var models = Training.trainFrom(csv).orElseThrow();
		assertFalse(Training.save(models, tempDir).isError());
		assertTrue(new File(tempDir, Target.HEAT_DEMAND.modelFile()).isFile());
		assertTrue(new File(tempDir, Target.PEAK_LOAD.modelFile()).isFile());

		var predictor = new BoostPredictor(
			models.heatDemand(),
			models.peakLoad()
		);
		var building = new Building()
			.groundArea(100)
			.height(10)
			.type(BuildingType.SINGLE_FAMILY)
			.constructionAge(ConstructionAge.AGE_1979_1995)
			.roofTypeCode("1000");
		var prediction = predictor
			.predict(new ClimateRegion().number(5), building)
			.orElseThrow();
		assertTrue(prediction.heatDemand() > 0);
		assertTrue(prediction.peakLoad() > 0);

		var rows = CsvItem.readFrom(csv).orElseThrow().size();
		var validation = ModelValidator.validate(predictor, csv).orElseThrow();
		assertEquals(rows, validation.heatDemand().values().size());
		assertEquals(rows, validation.peakLoad().values().size());
	}
}
