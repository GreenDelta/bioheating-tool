package com.greendelta.bioheating.model;

/// The building type. There is no unknown value; when the type cannot be
/// determined it defaults to MULTI_GENERATION.
public enum BuildingType {
	HIGH_RISE(1),
	MULTI_FAMILY_SMALL(2),
	MULTI_FAMILY_MEDIUM(3),
	MULTI_FAMILY_LARGE(4),
	BUILDING_PART(5),
	SINGLE_FAMILY(6),
	END_TERRACE(7),
	MID_TERRACE(8),
	HOUSE_GROUP(9),
	MULTI_GENERATION(10);

	private final int code;

	BuildingType(int code) {
		this.code = code;
	}

	public int code() {
		return code;
	}

	public static BuildingType of(int code) {
		for (var t : values()) {
			if (t.code == code) return t;
		}
		return MULTI_GENERATION;
	}
}
