package com.greendelta.bioheating.predict;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.openlca.commons.Res;

/// Contains training or validation data of a building for the heat demand
/// prediction model.
///
/// @param buildingId Column 0 - a unique identifier.
/// @param height Column 1 - the height in meters.
/// @param storeys Column 2 - the number of storeys.
/// @param groundArea Column 3 - the ground area in square meters.
/// @param buildingTypeCode Column 4 - the building type code.
/// @param climateRegionCode Column 5 - the climate region code.
/// @param constructionAgeCode Column 6 - the construction age code.
/// @param roofTypeFactor Column 7 - the roof type factor.
/// @param heatDemand Column 8 - the actual or predicted heat demand in kWh.
public record CsvItem(
	String buildingId,
	double height,
	int storeys,
	double groundArea,
	int buildingTypeCode,
	int climateRegionCode,
	int constructionAgeCode,
	double roofTypeFactor,
	double heatDemand
) {
	public static Res<List<CsvItem>> readFrom(File file) {
		if (file == null) return Res.error("No file provided");
		try {
			var items = new ArrayList<CsvItem>();
			var first = true;
			for (var line : Files.readAllLines(file.toPath())) {
				if (first) {
					first = false;
					continue;
				}
				var row = line.split(",");
				var item = fromRow(row);
				if (item.isError()) return Res.error(
					"Failed to read CSV row " + (items.size() + 1)
				);
				items.add(item.value());
			}
			return Res.ok(items);
		} catch (Exception e) {
			return Res.error("Failed to read CSV file", e);
		}
	}

	private static Res<CsvItem> fromRow(String[] row) {
		try {
			if (row.length < 9) return Res.error("CSV row has insufficient columns");

			var id = row[0].strip();
			if (id.startsWith("\"") && id.endsWith("\"")) {
				id = id.substring(1, id.length() - 1);
			}

			return Res.ok(
				new CsvItem(
					id,
					Double.parseDouble(row[1]), // height
					Integer.parseInt(row[2]), // storeys
					Double.parseDouble(row[3]), // ground area
					Integer.parseInt(row[4]), // building type code
					Integer.parseInt(row[5]), // climate region code
					Integer.parseInt(row[6]), // construction age code
					Double.parseDouble(row[7]), // roof type factor
					Double.parseDouble(row[8]) // heat demand
				)
			);
		} catch (Exception e) {
			return Res.error("Failed to parse CSV item", e);
		}
	}
}
