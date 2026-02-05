package com.greendelta.bioheating.io.sophena;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;

public record SophenaBuildingState(
	String id,
	String name,
	SophenaBuildingType type,
	double loadHours,
	boolean isDefault
) {
	public static SophenaBuildingState defaultOf(
		List<SophenaBuildingState> states,
		SophenaBuildingType type
	) {
		if (states == null || type == null) return null;
		for (var state : states) {
			if (state.isDefault() && state.type() == type) {
				return state;
			}
		}
		return null;
	}

	public static SophenaBuildingState bestMatch(
		List<SophenaBuildingState> states,
		SophenaBuildingType type,
		double loadHours
	) {
		if (states == null || type == null) return null;
		SophenaBuildingState best = null;
		double minDiff = Double.MAX_VALUE;
		for (var state : states) {
			if (state.type() != type) continue;
			double diff = Math.abs(state.loadHours() - loadHours);
			if (diff < minDiff) {
				minDiff = diff;
				best = state;
			}
		}
		return best;
	}

	public static List<SophenaBuildingState> getAll() {
		var stream = SophenaBuildingState.class.getResourceAsStream(
			"building_states.csv"
		);
		if (stream == null) return List.of();

		var format = CSVFormat.DEFAULT.builder()
			.setDelimiter(';')
			.setHeader()
			.setSkipHeaderRecord(true)
			.setTrim(true)
			.get();

		try (
			var reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
			var parser = format.parse(reader)
		) {
			var list = new ArrayList<SophenaBuildingState>();
			for (var row : parser) {
				if (row.size() < 8) continue;

				var id = row.get(0);
				var name = row.get(1);
				var typeStr = row.get(2);
				var loadHours = Double.parseDouble(row.get(6));
				var isDefault = Boolean.parseBoolean(row.get(7));

				var type = SophenaBuildingType.OTHER;
				try {
					type = SophenaBuildingType.valueOf(typeStr);
				} catch (Exception ignored) {}

				list.add(
					new SophenaBuildingState(id, name, type, loadHours, isDefault)
				);
			}
			return list;
		} catch (Exception e) {
			return List.of();
		}
	}
}
