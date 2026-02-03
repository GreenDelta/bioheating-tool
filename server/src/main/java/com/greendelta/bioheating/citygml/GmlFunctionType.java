package com.greendelta.bioheating.citygml;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

public record GmlFunctionType(String code, String label, boolean isHeated) {
	public static Map<String, GmlFunctionType> getAll() {
		var stream = GmlFunctionType.class.getResourceAsStream(
			"function-types.csv"
		);
		if (stream == null) return Map.of();

		try (
			stream;
			var reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
			var parser = CSVParser.parse(reader, CSVFormat.DEFAULT)
		) {
			var types = new HashMap<String, GmlFunctionType>();
			boolean header = true;
			for (var row : parser) {
				if (header) {
					header = false;
					continue;
				}

				if (row.size() < 3) continue;
				var code = row.get(0);
				var function = row.get(1);
				var heated = row.get(2);
				var isHeated = heated.startsWith("t");
				if (code != null && !code.isBlank()) {
					types.put(code, new GmlFunctionType(code, function, isHeated));
				}
			}
			return types;
		} catch (Exception e) {
			return Map.of();
		}
	}
}
