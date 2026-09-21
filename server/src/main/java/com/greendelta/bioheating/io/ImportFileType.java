package com.greendelta.bioheating.io;

import java.io.File;
import java.util.Locale;

/// The file types that can be imported as building data. Uploaded files that
/// do not match one of these types are skipped during an import.
public enum ImportFileType {

	/// CityGML data in a `gml`, `xml`, or `citygml` file.
	CITY_GML,

	/// Building data in an Excel `xlsx` file.
	EXCEL;

	/// Returns the import type of the given file, or `null` when the file is
	/// `null` or does not have a supported extension.
	public static ImportFileType of(File file) {
		return file == null ? null : ofName(file.getName());
	}

	/// Returns the import type for the given file name, or `null` when the name
	/// is `null`, empty, does not have a supported extension, or refers to a
	/// macOS metadata entry.
	public static ImportFileType ofName(String fileName) {
		if (fileName == null) return null;
		var name = fileName.replace('\\', '/').toLowerCase(Locale.ROOT).strip();
		if (name.isEmpty()) return null;

		// macOS adds these entries to ZIP archives and directly uploaded
		// folders. Note that an AppleDouble entry like `__MACOSX/._model.gml`
		// ends with `.gml` and would otherwise be parsed as CityGML.
		if (name.contains("__macosx/")) return null;
		var slash = name.lastIndexOf('/');
		var baseName = slash >= 0 ? name.substring(slash + 1) : name;
		if (baseName.isEmpty() || baseName.startsWith("._")) return null;

		if (baseName.endsWith(".xlsx")) return EXCEL;
		if (baseName.endsWith(".gml")
			|| baseName.endsWith(".xml")
			|| baseName.endsWith(".citygml")) return CITY_GML;
		return null;
	}

	/// Returns true when the given file name refers to an importable file.
	public static boolean isSupported(String fileName) {
		return ofName(fileName) != null;
	}
}
