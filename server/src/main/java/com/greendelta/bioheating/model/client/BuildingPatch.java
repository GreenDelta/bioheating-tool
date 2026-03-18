package com.greendelta.bioheating.model.client;

import com.greendelta.bioheating.model.Building;

import java.util.Map;
import java.util.function.Function;

record BuildingPatch(Map<String, Object> properties) {

	BuildingPatch of(GeoFeature f) {
		return new BuildingPatch(f.properties());
	}

	void applyOn(Building b) {
		if (b == null || properties == null) {
			return;
		}

		setString("name", b::name);
		setString("roofTypeCode", b::roofTypeCode);
		setString("roofTypeLabel", b::roofTypeLabel);
		setString("functionCode", b::functionCode);
		setString("functionLabel", b::functionLabel);

		setString("country", b::country);
		setString("locality", b::locality);
		setString("postalCode", b::postalCode);
		setString("street", b::street);
		setString("streetNumber", b::streetNumber);

	}

	private void setString(String key, Function<String, ?> setter) {
		var value = properties.get(key) instanceof String s ? s : null;
		setter.apply(value);
	}


}
