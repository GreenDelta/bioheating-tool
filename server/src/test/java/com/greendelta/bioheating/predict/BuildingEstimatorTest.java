package com.greendelta.bioheating.predict;

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

class BuildingEstimatorTest {

	@TempDir
	File tempDir;

	@Test
	void estimatesHeatDemandAndPeakLoadForSingleBuilding() throws Exception {
		var models = Training.trainFrom(TestData.writeCsv(tempDir)).orElseThrow();
		var estimator = new BuildingEstimator(
			new BoostPredictor(models.heatDemand(), models.peakLoad())
		);

		var building = new Building()
			.groundArea(120)
			.height(9.5)
			.type(BuildingType.MULTI_FAMILY_SMALL)
			.constructionAge(ConstructionAge.AGE_1949_1978)
			.roofType(RoofType.FLAT)
			.isHeated(true);
		var region = new ClimateRegion().number(5);

		var result = estimator.estimate(region, building);
		assertFalse(result.isError());
		assertTrue(result.value().heatDemand() > 0);
		assertTrue(result.value().peakLoad() > 0);
	}
}
