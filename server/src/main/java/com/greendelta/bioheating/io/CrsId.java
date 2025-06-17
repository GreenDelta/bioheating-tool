package com.greendelta.bioheating.io;

import java.util.OptionalInt;

import com.greendelta.bioheating.util.Strings;

public record CrsId(int code, String value) {

	/// [EPSG:4326](https://epsg.io/4326), this is the standard CRS in GeoJSON.
	/// Also, Open-Street-Map data are provided in this CRS.
	public static CrsId wgs84() {
		return new CrsId(4326, "EPSG:4326");
	}

	/// [EPSG:25832](https://epsg.io/25832), ETRS89 / UTM zone 32N, often used
	/// for CityGML data in Germany.
	public static CrsId utm32() {
		return new CrsId(25832, "EPSG:25832");
	}

	/// [EPSG:25833](https://epsg.io/25833), ETRS89 / UTM zone 33N, often used
	/// for CityGML data in Germany.
	public static CrsId utm33() {
		return new CrsId(25833, "EPSG:25833");
	}

	public static CrsId of(int code) {
		return new CrsId(code, "EPSG:" + code);
	}

	@Override
	public int hashCode() {
		return code;
	}

	@Override
	public String toString() {
		return value;
	}

	public boolean isValid() {
		return code > 0 && !Strings.isNil(value);
	}

	public static CrsId parse(String id) {
		if (id == null || id.isBlank())
			return new CrsId(-1, "");

		var part = new StringBuilder();
		boolean isNextCode = false;
		for (int i = 0; i < id.length(); i++) {
			var c = id.charAt(i);
			if (isPart(c)) {
				part.append(c);
				continue;
			}

			if (isNextCode) {
				var code = eatInt(part);
				if (code.isPresent())
					return CrsId.of(code.getAsInt());
				continue;
			}

			var next = part.toString();
			part.setLength(0);
			switch (next.toUpperCase()) {
				case "EPSG" -> isNextCode = true;
				case "ETRS89_UTM32" -> {
					return utm32();
				}
				case "ETRS89_UTM33" -> {
					return utm33();
				}
			}
		}

		if (!part.isEmpty()) {
			if (isNextCode) {
				var code = eatInt(part);
				if (code.isPresent())
					return CrsId.of(code.getAsInt());
			} else {
				switch (part.toString().toUpperCase()) {
					case "ETRS89_UTM32" -> {
						return utm32();
					}
					case "ETRS89_UTM33" -> {
						return utm33();
					}
				}
			}
		}

		return new CrsId(-1, id);
	}

	private static boolean isPart(char c) {
		return !Character.isWhitespace(c)
			&& c != ':'
			&& c != ','
			&& c != '*';
	}

	private static OptionalInt eatInt(StringBuilder buff) {
		if (buff.isEmpty())
			return OptionalInt.empty();
		var s = buff.toString();
		buff.setLength(0);
		try {
			int i = Integer.parseInt(s);
			return OptionalInt.of(i);
		} catch (NumberFormatException e) {
			return OptionalInt.empty();
		}
	}
}
