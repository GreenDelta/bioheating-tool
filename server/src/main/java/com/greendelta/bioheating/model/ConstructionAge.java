package com.greendelta.bioheating.model;

public enum ConstructionAge {

	UNKNOWN(0, "unknown", 130),
	AGE_1900_1919(1, "1900-1919", 180),
	AGE_1919_1948(2, "1919-1948", 190),
	AGE_1949_1978(3, "1949-1978", 210),
	AGE_1979_1995(4, "1979-1995", 150),
	AGE_1995_2009(5, "1995-2009", 80),
	AGE_2010_2030(6, "2010-2030", 50);

	final int code;
	final String label;
	final double averageHeatDemand;

	ConstructionAge(int code, String label, double averageHeatDemand) {
		this.code = code;
		this.label = label;
		this.averageHeatDemand = averageHeatDemand;
	}

	public int code() {
		return code;
	}

	/// Returns the average annual heat demand of a building of this age in
	/// kWh/m2/year.
	public double averageHeatDemand() {
		return averageHeatDemand;
	}

	@Override
	public String toString() {
		return label;
	}

	public static ConstructionAge ofCode(int code) {
		for (ConstructionAge age : values()) {
			if (age.code == code) {
				return age;
			}
		}
		return UNKNOWN;
	}

	public static ConstructionAge fromString(String label) {
		if (label == null || label.isEmpty()) {
			return UNKNOWN;
		}
		for (ConstructionAge age : values()) {
			if (age.label.equals(label)) {
				return age;
			}
		}
		return UNKNOWN;
	}

	public static ConstructionAge ofYear(int year) {
		if (year < 1900)
			return UNKNOWN;
		if (year <= 1919)
			return AGE_1900_1919;
		if (year <= 1948)
			return AGE_1919_1948;
		if (year <= 1978)
			return AGE_1949_1978;
		if (year <= 1995)
			return AGE_1979_1995;
		if (year <= 2009)
			return AGE_1995_2009;
		if (year <= 2030)
			return AGE_2010_2030;
		return UNKNOWN;
	}
}
