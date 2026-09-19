package com.greendelta.bioheating.predict;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

/// Writes small, artificial CSV files for tests.
final class TestData {

	private TestData() {}

	static File writeCsv(File dir) throws IOException {
		var file = new File(dir, "test-data.csv");
		Files.writeString(file.toPath(), csv());
		return file;
	}

	private static String csv() {
		var lines = new ArrayList<String>();
		lines.add(
			"ground area [m2],height [m],weather station [code],"
			+ "construction year [code],roof type [1|0],building type [code],"
			+ "heat demand [kWh],peak load [kW]"
		);
		for (var i = 1; i <= 40; i++) {
			var groundArea = 40 + i * 10;
			var height = 3 + (i % 12);
			var heatDemand = groundArea * 50.0 + height * 20.0;
			var peakLoad = heatDemand / 20.0 + 5.0;
			lines.add(
				groundArea + "," + height + "," + (1 + i % 15) + ","
				+ (1 + i % 6) + "," + (i % 2) + "," + (1 + i % 10) + ","
				+ heatDemand + "," + peakLoad
			);
		}
		return String.join("\n", lines) + "\n";
	}
}
