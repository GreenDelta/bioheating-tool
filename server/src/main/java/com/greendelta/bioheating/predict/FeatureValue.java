package com.greendelta.bioheating.predict;

import com.greendelta.bioheating.model.Project;

public class FeatureValue {
	private FeatureValue() {
	}

	public static float ofClimateRegion(Project project) {
		return project != null && project.climateRegion() != null
			? ofClimateRegion(project.climateRegion().number())
			: 0.91f;
	}

	public static float ofClimateRegion(int code) {
		return switch (code) {
			case 1 -> 0.85f;
			case 2 -> 1.08f;
			case 3 -> 0.83f;
			case 4 -> 0.84f;
			case 5 -> 1.01f;
			case 6 -> 0.8f;
			case 7 -> 1.06f;
			case 8 -> 0.84f;
			case 9 -> 0.89f;
			case 10 -> 1.06f;
			case 11 -> 1.16f;
			case 12 -> 0.91f;
			case 13 -> 1.15f;
			case 14 -> 1.19f;
			case 15 -> 0.89f;
			default -> 0.91f;
		};
	}

}
