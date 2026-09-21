package com.greendelta.bioheating.model;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

/// Handles the warm water share of the heat demand of a building.
///
/// The warm water fraction depends on the building type and the construction
/// age; the values are stored in `warm-water-fractions.csv`. The heat demand
/// and the peak load of a building always include the warm water demand, which
/// is derived from that fraction with [#totalOf].
public final class WarmWater {

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

	/// Scales a space heating demand or peak load of the building so that it
	/// includes the warm water demand: `y = x / (1 - f / 100)` with the warm
	/// water fraction `f` of the building in percent.
	public static double totalOf(Building building, double spaceHeating) {
		if (building == null) return spaceHeating;
		var fraction = building.warmWaterFraction();
		return fraction > 0 && fraction < 100
			? spaceHeating / (1 - fraction / 100)
			: spaceHeating;
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
