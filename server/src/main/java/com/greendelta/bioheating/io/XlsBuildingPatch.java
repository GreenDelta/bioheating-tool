package com.greendelta.bioheating.io;

import com.greendelta.bioheating.model.BuildingType;
import com.greendelta.bioheating.model.ConstructionAge;
import java.util.Locale;
import org.apache.poi.ss.usermodel.Row;

/// One parsed row of an Excel import file. All values that are not provided are
/// `null`: blank text cells, missing cells, unknown codes, and numbers `<= 0`.
/// This way the import can distinguish a provided value from a missing one and
/// apply the merge rules (provided values overwrite; missing values keep the
/// existing value or fall back to a smart default).
///
/// The columns are read by their position in the order defined in
/// `doc/Excel-Format.md`.
record XlsBuildingPatch(
	String cityId,
	String name,
	double longitude,
	double latitude,
	BuildingType type,
	ConstructionAge constructionAge,
	Double height,
	Double groundArea,
	Boolean flatRoof,
	Double warmWaterFraction,
	Double heatDemand,
	Double peakLoad,
	String locality,
	String postalCode,
	String street,
	String streetNumber
) {

	/// Parses the given row, or returns `null` when the row is `null`.
	static XlsBuildingPatch of(Row row) {
		if (row == null) return null;
		return new XlsBuildingPatch(
			textOf(row, 0),
			textOf(row, 1),
			rawNumberOf(row, 2),
			rawNumberOf(row, 3),
			typeOf(row, 4),
			ConstructionAge.parse(textOf(row, 5)),
			numberOf(row, 6),
			numberOf(row, 7),
			boolOf(row, 8),
			numberOf(row, 9),
			numberOf(row, 10),
			numberOf(row, 11),
			textOf(row, 12),
			textOf(row, 13),
			textOf(row, 14),
			textOf(row, 15)
		);
	}

	/// The name that should be used when the name is not provided: the street
	/// and number, or the ID.
	String nameOrDefault() {
		if (name != null) return name;
		if (street == null) return cityId;
		return streetNumber == null ? street : street + " " + streetNumber;
	}

	/// A row is skipped when it has no valid coordinates or when no name can be
	/// derived from the name, the address or the ID.
	boolean isValid() {
		return hasValidCoordinates() && nameOrDefault() != null;
	}

	private boolean hasValidCoordinates() {
		return longitude != 0 && latitude != 0
			&& longitude >= -180 && longitude <= 180
			&& latitude >= -90 && latitude <= 90;
	}

	/// Reads a numeric value; `<= 0` means _not set_.
	private static Double numberOf(Row row, int column) {
		var value = rawNumberOf(row, column);
		return value > 0 ? value : null;
	}

	private static double rawNumberOf(Row row, int column) {
		var cell = row.getCell(column);
		if (cell == null) return 0;
		return switch (cell.getCellType()) {
			case NUMERIC -> cell.getNumericCellValue();
			case STRING -> numberOfText(cell.getStringCellValue());
			case BOOLEAN -> cell.getBooleanCellValue() ? 1 : 0;
			default -> 0;
		};
	}

	private static double numberOfText(String text) {
		if (text == null || text.isBlank()) return 0;
		try {
			return Double.parseDouble(text.strip());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/// Reads a boolean; `null` means _not set_.
	private static Boolean boolOf(Row row, int column) {
		var cell = row.getCell(column);
		if (cell == null) return null;
		return switch (cell.getCellType()) {
			case BOOLEAN -> cell.getBooleanCellValue();
			case NUMERIC -> cell.getNumericCellValue() != 0;
			case STRING -> boolOfText(cell.getStringCellValue());
			default -> null;
		};
	}

	private static Boolean boolOfText(String text) {
		if (text == null || text.isBlank()) return null;
		return switch (text.strip().toLowerCase(Locale.ROOT)) {
			case "x", "y", "yes", "true", "t", "j", "ja", "1" -> true;
			default -> false;
		};
	}

	/// Reads a building type; unknown or missing codes mean _not set_, so that
	/// the type can be estimated from the other attributes.
	private static BuildingType typeOf(Row row, int column) {
		int code = (int) rawNumberOf(row, column);
		if (code <= 0) return null;
		for (var type : BuildingType.values()) {
			if (type.code() == code) return type;
		}
		return null;
	}

	private static String textOf(Row row, int column) {
		var cell = row.getCell(column);
		if (cell == null) return null;
		var text = switch (cell.getCellType()) {
			case STRING -> cell.getStringCellValue();
			case NUMERIC -> numberText(cell.getNumericCellValue());
			case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
			default -> null;
		};
		if (text == null) return null;
		var trimmed = text.strip();
		return trimmed.isEmpty() ? null : trimmed;
	}

	/// Formats numeric cells without a trailing `.0` for integral values, so
	/// that numeric IDs and codes read as `12345` and not `12345.0`.
	private static String numberText(double value) {
		if (value == Math.rint(value)
			&& !Double.isNaN(value)
			&& !Double.isInfinite(value)) {
			return String.valueOf((long) value);
		}
		return String.valueOf(value);
	}
}
