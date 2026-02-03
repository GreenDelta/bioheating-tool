package com.greendelta.bioheating.citygml;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

public record GmlRoofType(String code, String label, double volumeFactor) {
	public static Map<String, GmlRoofType> getAll() {
		var stream = GmlRoofType.class.getResourceAsStream("roof-types.csv");
		if (stream == null) return Map.of();

		try (
			stream;
			var reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
			var parser = CSVParser.parse(reader, CSVFormat.DEFAULT)
		) {
			var types = new HashMap<String, GmlRoofType>();
			boolean header = true;
			for (var row : parser) {
				if (header) {
					header = false;
					continue;
				}

				if (row.size() < 2) continue;
				var code = row.get(0);
				var label = row.get(1);
				var factor = Double.parseDouble(row.get(2));
				types.put(code, new GmlRoofType(code, label, factor));
			}
			return types;
		} catch (Exception e) {
			return Map.of();
		}
	}
}
