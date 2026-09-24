package com.greendelta.bioheating.model;

import java.util.function.IntSupplier;

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

	public static BuildingType fromCode(int code) {
		for (var t : values()) {
			if (t.code == code) return t;
		}
		return MULTI_GENERATION;
	}

	public static BuildingType estimateFrom(double height, double groundArea) {
		return estimateFrom(height, groundArea, null);
	}

	/// Tries to estimate the building type from the given attribute values.
	/// Values `<= 0` or `null` are interpreted as _not provided_ and ignored or
	/// handled as default.
	public static BuildingType estimateFrom(
		double height, double groundArea, IntSupplier neighborCount
	) {
		boolean hasHeight = height > 0;
		boolean hasArea = groundArea > 0;

		// no geometry: only the neighbor count is left
		if (!hasHeight && !hasArea)
			return byNeighbors(neighborCount);

		// only the ground area is known
		if (!hasHeight) {
			if (groundArea < 30)
				return BuildingType.BUILDING_PART;
			if (groundArea < 150)
				return byNeighbors(neighborCount);
			if (groundArea < 380)
				return BuildingType.MULTI_FAMILY_MEDIUM;
			else
				return BuildingType.MULTI_FAMILY_LARGE;
		}

		// only the height is known
		if (!hasArea) {
			if (height > 25)
				return BuildingType.HIGH_RISE;
			if (height > 18)
				return BuildingType.MULTI_FAMILY_LARGE;
			if (height > 12)
				return BuildingType.MULTI_FAMILY_MEDIUM;
			return byNeighbors(neighborCount);
		}

		// both values are known
		if (height > 25)
			return BuildingType.HIGH_RISE;
		if (height > 12) {
			var block = height * groundArea;
			if (block < 2000)
				return BuildingType.MULTI_FAMILY_SMALL;
			if (block < 5000)
				return BuildingType.MULTI_FAMILY_MEDIUM;
			else
				return BuildingType.MULTI_FAMILY_LARGE;
		}

		if (groundArea > 150)
			return BuildingType.MULTI_FAMILY_SMALL;
		if (groundArea < 30)
			return BuildingType.BUILDING_PART;
		else
			return byNeighbors(neighborCount);
	}

	private static BuildingType byNeighbors(IntSupplier count) {
		if (count == null)
			return BuildingType.SINGLE_FAMILY;
		return switch (Math.max(0, count.getAsInt())) {
			case 0 -> BuildingType.SINGLE_FAMILY;
			case 1 -> BuildingType.END_TERRACE;
			case 2 -> BuildingType.MID_TERRACE;
			default -> BuildingType.HOUSE_GROUP;
		};
	}

}
