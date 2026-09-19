package com.greendelta.bioheating.model;

/// The roof type of a building. The prediction model only distinguishes
/// between flat and pitched roofs.
public enum RoofType {

	FLAT(1),

	PITCHED(0);

	private final int code;

	RoofType(int code) {
		this.code = code;
	}

	/// The code that is used as model input.
	public int code() {
		return code;
	}
}
