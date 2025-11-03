package com.greendelta.bioheating.model;

public enum BuildingType {

	HIGH_RISE(1, 3.3, 0.75, 0.65),
	MULTI_FAMILY_SMALL(2, 3.0, 0.8, 0.80),
	MULTI_FAMILY_MEDIUM(3, 2.9, 0.78, 0.75),
	MULTI_FAMILY_LARGE(4, 3.1, 0.75, 0.70),
	BUILDING_PART(5, 3.0, 0.8, 0.80),
	SINGLE_FAMILY(6, 2.8, 0.8, 1.00),
	END_TERRACE(7, 2.75, 0.85, 0.90),
	MID_TERRACE(8, 2.7, 0.87, 0.80),
	HOUSE_GROUP(9, 2.75, 0.83, 0.88),
	OTHER(0, 2.85, 0.8, 0.8);

	private final int code;
	private final double defaultStoreyHeight;
	private final double heatedAreaFactor;
	private final double typeFactor;

	BuildingType(
		int code,
		double defaultStoreyHeight,
		double heatedAreaFactor,
		double typeFactor) {
		this.code = code;
		this.defaultStoreyHeight = defaultStoreyHeight;
		this.heatedAreaFactor = heatedAreaFactor;
		this.typeFactor = typeFactor;
	}

	public int code() {
		return code;
	}

	public double defaultStoreyHeight() {
		return defaultStoreyHeight;
	}

	public double heatedAreaFactor() {
		return heatedAreaFactor;
	}

	public double typeFactor() {
		return typeFactor;
	}

	public static BuildingType of(int code) {
		for (var t : values()) {
			if (t.code == code)
				return t;
		}
		return OTHER;
	}
}
