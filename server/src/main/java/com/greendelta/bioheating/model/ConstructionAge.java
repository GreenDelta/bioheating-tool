package com.greendelta.bioheating.model;

/// The construction age of a building. There is no unknown value; when the
/// age cannot be determined it defaults to 1979-1995.
public enum ConstructionAge {
	AGE_1900_1919(1, "1900-1919"),
	AGE_1919_1948(2, "1919-1948"),
	AGE_1949_1978(3, "1949-1978"),
	AGE_1979_1995(4, "1979-1995"),
	AGE_1995_2009(5, "1995-2009"),
	AGE_2010_2030(6, "2010-2030");

	final int code;
	final String label;

	ConstructionAge(int code, String label) {
		this.code = code;
		this.label = label;
	}

	public int code() {
		return code;
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
		return AGE_1979_1995;
	}

	public static ConstructionAge fromString(String label) {
		if (label == null || label.isEmpty()) {
			return AGE_1979_1995;
		}
		for (ConstructionAge age : values()) {
			if (age.label.equals(label)) {
				return age;
			}
		}
		return AGE_1979_1995;
	}

	public static ConstructionAge ofYear(int year) {
		if (year < 1900) return AGE_1979_1995;
		if (year <= 1919) return AGE_1900_1919;
		if (year <= 1948) return AGE_1919_1948;
		if (year <= 1978) return AGE_1949_1978;
		if (year <= 1995) return AGE_1979_1995;
		if (year <= 2009) return AGE_1995_2009;
		if (year <= 2030) return AGE_2010_2030;
		return AGE_1979_1995;
	}

	/// Parses a construction age from a user provided value. The following forms
	/// are accepted:
	///
	/// - a code `< 7`, for example `4`
	/// - a year, for example `1994`
	/// - a range string, for example `1979-1994`
	///
	/// Returns `null` when the value is `null`, blank, or cannot be parsed, so
	/// that the caller can fall back to a default.
	public static ConstructionAge parse(String value) {
		if (value == null || value.isBlank()) return null;
		var text = value.strip();

		// a range string like `1979-1994`: the ranges are defined by their upper
		// bound, so we use the second number
		var dash = text.indexOf('-');
		if (dash > 0 && dash < text.length() - 1) {
			var end = numberOf(text.substring(dash + 1));
			return end > 0 ? ofYear((int) end) : null;
		}

		var number = numberOf(text);
		if (Double.isNaN(number) || number <= 0) return null;
		return number < 7 ? ofCode((int) number) : ofYear((int) number);
	}

	private static double numberOf(String value) {
		if (value == null || value.isBlank()) return Double.NaN;
		try {
			return Double.parseDouble(value.strip());
		} catch (NumberFormatException e) {
			return Double.NaN;
		}
	}
}
