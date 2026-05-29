package com.greendelta.bioheating.predict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.BuildingType;
import com.greendelta.bioheating.model.ClimateRegion;
import com.greendelta.bioheating.model.ConstructionAge;
import org.junit.jupiter.api.Test;

class BuildingEstimatorTest {

	@Test
	void estimatesHeatDemandForSingleBuilding() {
		var estimator = BuildingEstimator.getDefault();
		assertFalse(estimator.isError());

		var building = new Building()
			.height(9.5)
			.storeys(3)
			.groundArea(120)
			.type(BuildingType.MULTI_FAMILY_SMALL)
			.constructionAge(ConstructionAge.AGE_1949_1978)
			.roofTypeCode("1000")
			.isHeated(true);
		var region = new ClimateRegion().number(5);

		var result = estimator.value().estimate(region, building);
		assertFalse(result.isError());
		assertTrue(result.value().heatDemand() > 0);
		assertEquals(
			BuildingEstimator.peakLoadOf(result.value().heatDemand()),
			result.value().peakLoad(),
			1e-6
		);
	}
}
