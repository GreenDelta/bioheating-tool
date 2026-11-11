package com.greendelta.bioheating.model;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

/// Handles the warm water share of the heat demand of a building.
///
/// The warm water fraction is the share of the warm water demand in the total
/// annual heat demand of a building, in percent. It depends on the building
/// type and the construction age. The heat demand and the peak load of a
/// building always include the warm water demand.
///
/// The warm water demand is modelled as a constant base load over all hours of
/// the year, below the temperature dependent space heating load. Thus, it adds
/// its full share to the annual heat demand but only this constant base load
/// to the peak load. This is the same model that Sophena uses to calculate the
/// load curve of a consumer.
public final class WarmWater {

	/// The number of hours of a year over which the warm water demand is
	/// distributed as a constant base load.
	private static final double HOURS_PER_YEAR = 8760;

	/// The warm water fraction in percent that is used when no other value is
	/// available.
	public static final double DEFAULT = 14;

	private static final Map<Key, Double> FRACTIONS = load();

	private WarmWater() {}

	/// The warm water fraction in percent for the given building type and
	/// construction age. When the type or the age is not known, the default
	/// values (MULTI_GENERATION and AGE_1979_1995) are used.
	public static double of(BuildingType type, ConstructionAge age) {
		var typeCode = type == null
			? BuildingType.MULTI_GENERATION.code()
			: type.code();
		var ageCode = age == null
			? ConstructionAge.AGE_1979_1995.code()
			: age.code();
		var value = FRACTIONS.get(new Key(typeCode, ageCode));
		return value != null ? value : DEFAULT;
	}

	/// Adds the warm water demand to the annual space heating demand of the
	/// building: `Q = Q_space / (1 - f / 100)` with the warm water fraction `f`
	/// of the building. Returns the total annual heat demand in the unit of the
	/// given value; the building is not modified.
	public static double addToDemand(Building building, double spaceDemand) {
		if (building == null) return spaceDemand;
		var fraction = building.warmWaterFraction();
		return fraction > 0 && fraction < 100
			? spaceDemand / (1 - fraction / 100)
			: spaceDemand;
	}

	/// Adds the constant warm water base load to the space heating peak load of
	/// the building: `P = P_space + (Q - Q_space) / 8760`, where `Q - Q_space` is
	/// the annual warm water demand derived from the annual space heating demand
	/// `Q_space`. The peak load is expected in kW and the demand in kWh. Returns
	/// the total peak load in kW; the building is not modified.
	public static double addToPeakLoad(
		Building building, double spacePeakLoad, double spaceDemand
	) {
		var waterDemand = addToDemand(building, spaceDemand) - spaceDemand;
		return spacePeakLoad + waterDemand / HOURS_PER_YEAR;
	}

	private static Map<Key, Double> load() {
		var stream = WarmWater.class.getResourceAsStream(
			"warm-water-fractions.csv"
		);
		if (stream == null) return Map.of();

		try (
			stream;
			var reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
			var parser = CSVParser.parse(reader, CSVFormat.DEFAULT)
		) {
			var fractions = new HashMap<Key, Double>();
			boolean header = true;
			for (var row : parser) {
				if (header) {
					header = false;
					continue;
				}
				if (row.size() < 3) continue;
				var key = new Key(
					Integer.parseInt(row.get(0).strip()),
					Integer.parseInt(row.get(1).strip())
				);
				fractions.put(key, Double.parseDouble(row.get(2).strip()));
			}
			return fractions;
		} catch (Exception e) {
			return Map.of();
		}
	}

	private record Key(int type, int age) {}
}
