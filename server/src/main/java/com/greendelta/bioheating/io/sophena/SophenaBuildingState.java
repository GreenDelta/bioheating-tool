package com.greendelta.bioheating.io.sophena;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;

public record SophenaBuildingState(
	String id,
	String name,
	SophenaBuildingType buildingType,
	double loadHours,
	boolean isDefault
) {
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

				var buildingType = SophenaBuildingType.OTHER;
				try {
					buildingType = SophenaBuildingType.valueOf(typeStr);
				} catch (Exception ignored) {}

				list.add(
					new SophenaBuildingState(id, name, buildingType, loadHours, isDefault)
				);
			}
			return list;
		} catch (Exception e) {
			return List.of();
		}
	}
}
