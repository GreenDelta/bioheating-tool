package com.greendelta.bioheating.model;

import java.util.function.IntSupplier;

/// The geometry attributes that are used to estimate the heat demand and the
/// peak load of a building: the building type, the height in m, and the ground
/// area in m2.
///
/// Values `<= 0` are interpreted as _not provided_. [#resolve] fills these with
/// smart defaults: when no type is given it is estimated from the height, the
/// ground area and the number of heated neighbors; when the height or the ground
/// area is not given it is estimated from the type.
///
/// @param type the resolved building type, never `null`
/// @param height the resolved height in m, always greater than 0
/// @param groundArea the resolved ground area in m2, always greater than 0
public record BuildingDefaults(
	BuildingType type,
	double height,
	double groundArea
) {

	/// Resolves the building type and the geometry attributes of a building.
	/// Values `<= 0` for the height and the ground area are interpreted as
	/// _not provided_.
	///
	/// @param type the building type from the data, or `null` when it is not
	///             provided
	/// @param height the height in m, or `<= 0` when it is not provided
	/// @param groundArea the ground area in m2, or `<= 0` when it is not provided
	/// @param neighborCount the number of heated neighbors of the building
	public static BuildingDefaults resolve(
		BuildingType type,
		double height,
		double groundArea,
		int neighborCount
	) {
		return resolve(type, height, groundArea, () -> neighborCount);
	}

	/// Same as [#resolve(BuildingType, double, double, int)] but with a lazy
	/// supplier for the neighbor count. The supplier is only called when the type
	/// has to be estimated and the height and ground area are not decisive.
	///
	/// @param neighborCount supplies the number of heated neighbors of the
	///                     building
	public static BuildingDefaults resolve(
		BuildingType type,
		double height,
		double groundArea,
		IntSupplier neighborCount
	) {
		var resolvedType = type != null
			? type
			: BuildingType.estimateFrom(height, groundArea, neighborCount);
		var resolvedHeight = height > 0
			? height
			: resolvedType.defaultHeight();
		var resolvedArea = groundArea > 0
			? groundArea
			: resolvedType.defaultGroundArea();
		return new BuildingDefaults(resolvedType, resolvedHeight, resolvedArea);
	}
}
