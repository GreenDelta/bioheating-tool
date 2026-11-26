package com.greendelta.bioheating.model;

import java.util.function.IntSupplier;

/// The building type. There is no unknown value: an invalid or missing type
/// code defaults to MULTI_GENERATION (see `fromCode`). When the type is
/// estimated from the geometry with `estimateFrom`, but neither the height nor
/// the ground area is known, the number of heated neighbors decides, and
/// without neighbors it is a SINGLE_FAMILY house.
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

	/// Returns the default height in m of the respective building type. The
	/// default height and ground area of a type are chosen so that
	/// `estimateFrom` classifies them again as that type.
	public double defaultHeight() {
		return switch (this) {
			case HIGH_RISE -> 30;
			case MULTI_FAMILY_SMALL -> 14;
			case MULTI_FAMILY_MEDIUM -> 15;
			case MULTI_FAMILY_LARGE -> 20;
			case BUILDING_PART,
			     SINGLE_FAMILY,
			     END_TERRACE,
			     MID_TERRACE,
			     HOUSE_GROUP,
			     MULTI_GENERATION -> 8;
		};
	}

	/// Returns the default ground area in m2 of the respective building type.
	public double defaultGroundArea() {
		return switch (this) {
			case HIGH_RISE -> 400;
			case MULTI_FAMILY_SMALL -> 140;
			case MULTI_FAMILY_MEDIUM -> 300;
			case MULTI_FAMILY_LARGE -> 500;
			case BUILDING_PART -> 20;
			case SINGLE_FAMILY, HOUSE_GROUP -> 100;
			case END_TERRACE -> 90;
			case MID_TERRACE -> 80;
			case MULTI_GENERATION -> 200;
		};
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

		// only one value is known: the thresholds are chosen so that the
		// default value of the estimated type for the missing value (e.g. the
		// default height of 15 m for a medium multi-family house) gives the
		// same type in the full decision tree below

		// only the ground area is known
		if (!hasHeight) {
			if (groundArea < 30)
				return BuildingType.BUILDING_PART;
			if (groundArea < 150)
				return byNeighbors(neighborCount);
			if (groundArea < 333) // 333 m2 * 15 m < 5000 m3
				return BuildingType.MULTI_FAMILY_MEDIUM;
			else
				return BuildingType.MULTI_FAMILY_LARGE;
		}

		// only the height is known
		if (!hasArea) {
			if (height > 25)
				return BuildingType.HIGH_RISE;
			if (height > 16.5) // 16.5 m * 300 m2 < 5000 m3
				return BuildingType.MULTI_FAMILY_LARGE;
			if (height > 12)
				return BuildingType.MULTI_FAMILY_MEDIUM;
			return byNeighbors(neighborCount);
		}

		// both values are known
		if (height > 25)
			return BuildingType.HIGH_RISE;
		var volume = height * groundArea;
		if (height > 12) {
			if (volume < 2000)
				return BuildingType.MULTI_FAMILY_SMALL;
			if (volume < 5000)
				return BuildingType.MULTI_FAMILY_MEDIUM;
			else
				return BuildingType.MULTI_FAMILY_LARGE;
		}

		// low-rise buildings (height <= 12 m)
		if (volume > 2000)
			return BuildingType.MULTI_FAMILY_SMALL;
		if (volume > 1200)
			return BuildingType.MULTI_GENERATION;
		if (groundArea > 150)
			return BuildingType.MULTI_GENERATION;
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
