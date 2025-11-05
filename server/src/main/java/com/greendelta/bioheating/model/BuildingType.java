package com.greendelta.bioheating.model;

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
	OTHER(0);

	private final int code;

	BuildingType(int code) {
		this.code = code;
	}

	public int code() {
		return code;
	}

	public static BuildingType of(int code) {
		for (var t : values()) {
			if (t.code == code)
				return t;
		}
		return OTHER;
	}
}
