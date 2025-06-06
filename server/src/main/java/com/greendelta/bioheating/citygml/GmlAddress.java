package com.greendelta.bioheating.citygml;

import java.util.List;

import org.citygml4j.core.model.building.Building;
import org.xmlobjects.xal.model.Address;
import org.xmlobjects.xal.model.types.Name;

public record GmlAddress(
	String country,
	String locality,
	String postalCode,
	String street,
	String number
) {

	static GmlAddress of(Building b) {
		if (b == null)
			return null;
		for (var prop : b.getAddresses()) {
			var obj = prop.getObject();
			if (obj == null || obj.getXALAddress() == null)
				continue;
			var a = ofXal(obj.getXALAddress().getObject());
			if (a != null)
				return a;
		}
		return null;
	}

	private static GmlAddress ofXal(Address a) {
		if (a == null)
			return null;
		var street = streetOf(a);
		if (street == null)
			return null;

		var country = a.getCountry() != null
			? strOf(a.getCountry().getNameElements())
			: null;
		var locality = a.getLocality() != null
			? strOf(a.getLocality().getNameElements())
			: null;
		var postalCode = postalCodeOf(a);
		var number = numberOf(a);

		return new GmlAddress(
			country, locality, postalCode, street, number);
	}

	private static String streetOf(Address a) {
		var t = a.getThoroughfare();
		if (t == null)
			return null;
		var name = new StringBuilder();
		for (var e : t.getNameElementOrNumber()) {
			var elem = e.getNameElement();
			if (elem == null)
				continue;
			var s = elem.getContent();
			if (s == null)
				continue;
			if (!name.isEmpty()) {
				name.append(", ");
			}
			name.append(s);
		}
		return name.isEmpty() ? null : name.toString();
	}

	private static String numberOf(Address a) {
		var t = a.getThoroughfare();
		if (t == null)
			return null;
		var number = new StringBuilder();
		for (var e : t.getNameElementOrNumber()) {
			var num = e.getNumber();
			if (num == null)
				continue;
			var s = num.getContent();
			if (s == null)
				continue;
			number.append(s);
		}
		return number.isEmpty() ? null : number.toString();
	}

	private static String postalCodeOf(Address a) {
		var code = a.getPostCode();
		if (code == null)
			return null;
		for (var id : code.getIdentifiers()) {
			if (id != null)
				return id.getContent();
		}
		return null;
	}

	private static String strOf(List<? extends Name<?>> names) {
		if (names == null)
			return null;
		for (var name : names) {
			var s = name.getContent();
			if (s != null)
				return s;
		}
		return null;
	}
}

