package com.greendelta.bioheating.citygml;

import java.util.List;

import org.citygml4j.core.model.building.Building;
import org.xmlobjects.xal.model.Address;
import org.xmlobjects.xal.model.types.Name;

public record GmlAddress(
	String country, String locality, String street, String number
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

		var county = a.getCountry() != null
			? strOf(a.getCountry().getNameElements())
			: null;
		var locality = a.getLocality() != null
			? strOf(a.getLocality().getNameElements())
			: null;
		var number = numberOf(a);

		return new GmlAddress(county, locality, street, number);
	}

	private static String streetOf(Address a) {
		var t = a.getThoroughfare();
		if (t == null)
			return null;
		for (var e : t.getNameElementOrNumber()) {
			var elem = e.getNameElement();
			if (elem == null)
				continue;
			var s = elem.getContent();
			if (s != null)
				return s;
		}
		return null;
	}

	private static String numberOf(Address a) {
		var t = a.getThoroughfare();
		if (t == null)
			return null;
		for (var e : t.getNameElementOrNumber()) {
			var num = e.getNumber();
			if (num == null)
				continue;
			var s = num.getContent();
			if (s != null)
				return s;
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

