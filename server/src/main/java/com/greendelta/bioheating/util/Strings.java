package com.greendelta.bioheating.util;

public class Strings {

	private Strings() {
	}

	/// Returns `true` when the given string is `null` or blank.
	public static boolean isNil(String s) {
		return s == null || s.isBlank();
	}

	public static boolean eq(String a, String b) {
		var nilA = isNil(a);
		var nilB = isNil(b);
		if (nilA && nilB)
			return true;
		if (nilA || nilB)
			return false;
		return a.strip().equalsIgnoreCase(b.strip());
	}
}
