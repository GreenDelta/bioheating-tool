package com.greendelta.bioheating.predict;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.openlca.commons.Res;

/// One row of training or validation data. The columns are read by their
/// position and the header row is always skipped, so the header names can be
/// changed freely. See `server/model-training/README.md` for the format.
///
/// @param groundArea Column 0 - the ground area in m2.
/// @param height Column 1 - the height in m.
/// @param weatherStation Column 2 - the weather station code.
/// @param constructionYear Column 3 - the construction year code.
/// @param roofType Column 4 - the roof type: 1 = flat, 0 = pitched.
/// @param buildingType Column 5 - the building type code.
/// @param heatDemand Column 6 - the annual heat demand in kWh.
/// @param peakLoad Column 7 - the peak heating load in kW.
public record CsvItem(
	double groundArea,
	double height,
	int weatherStation,
	int constructionYear,
	int roofType,
	int buildingType,
	double heatDemand,
	double peakLoad
) {

	private static final int COLUMNS = 8;

	/// Reads the data rows of the given CSV file.
	public static Res<List<CsvItem>> readFrom(File file) {
		if (file == null) return Res.error("No file provided");
		try {
			var items = new ArrayList<CsvItem>();
			var first = true;
			for (var line : Files.readAllLines(file.toPath())) {
				if (first) {
					first = false; // skip the header row
					continue;
				}
				if (line.isBlank()) continue;
				var item = fromRow(line.split(","));
				if (item.isError()) return item.wrapError(
					"Failed to read CSV row " + (items.size() + 2)
				);
				items.add(item.value());
			}
			return Res.ok(items);
		} catch (Exception e) {
			return Res.error("Failed to read CSV file", e);
		}
	}

	private static Res<CsvItem> fromRow(String[] row) {
		if (row.length < COLUMNS) return Res.error(
			"CSV row has " + row.length + " columns, expected " + COLUMNS
		);
		try {
			return Res.ok(
				new CsvItem(
					Double.parseDouble(row[0].strip()),
					Double.parseDouble(row[1].strip()),
					Integer.parseInt(row[2].strip()),
					Integer.parseInt(row[3].strip()),
					Integer.parseInt(row[4].strip()),
					Integer.parseInt(row[5].strip()),
					Double.parseDouble(row[6].strip()),
					Double.parseDouble(row[7].strip())
				)
			);
		} catch (Exception e) {
			return Res.error("Failed to parse CSV item", e);
		}
	}
}
