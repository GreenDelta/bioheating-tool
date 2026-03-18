package com.greendelta.bioheating.model.client;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.BuildingType;
import com.greendelta.bioheating.model.ConstructionAge;
import com.greendelta.bioheating.model.Street;

import java.util.Map;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.IntFunction;

record PropertyPatch(Map<String, Object> properties) {

	static PropertyPatch of(GeoFeature f) {
		return f != null && f.properties() != null
			? new PropertyPatch(f.properties())
			: new PropertyPatch(Map.of());
	}

	void applyOn(Building b) {
		if (b == null) 	return;

		setString("name", b::name);
		setString("roofTypeCode", b::roofTypeCode);
		setString("roofTypeLabel", b::roofTypeLabel);
		setString("functionCode", b::functionCode);
		setString("functionLabel", b::functionLabel);

		setBuildingType(b::type);
		setConstructionAge(b::constructionAge);
		setDouble("height", b::height);
		setInt("storeys", b::storeys);
		setDouble("groundArea", b::groundArea);

		setString("country", b::country);
		setString("locality", b::locality);
		setString("postalCode", b::postalCode);
		setString("street", b::street);
		setString("streetNumber", b::streetNumber);

		setDouble("heatDemand", b::heatDemand);
		setDouble("peakLoad", b::peakLoad);
		setBool("isHeated", b::isHeated);
		setBool("isSupplyCenter", b::isSupplyCenter);
		setBool("isIncluded", b::isIncluded);
	}

	void applyOn(Street s) {
		if (s == null) return;
		setString("name", s::name);
		setBool("isExcluded", s::isExcluded);
	}

	private void setString(String key, Function<String, ?> setter) {
		var value = properties.get(key) instanceof String s ? s : null;
		if (value != null) {
			setter.apply(value);
		}
	}

	private void setDouble(String key, DoubleFunction<?> setter) {
		if (properties.get(key) instanceof Number num) {
			setter.apply(num.doubleValue());
		}
	}

	private void setInt(String key, IntFunction<?> setter) {
		if (properties.get(key) instanceof Number num) {
			setter.apply(num.intValue());
		}
	}

	private void setBool(String key, Function<Boolean, ?> setter) {
		if (properties.get(key) instanceof Boolean b) {
			setter.apply(b);
		}
	}

	private void setBuildingType(Function<BuildingType, ?> setter) {
		var type = getEnum("type", BuildingType.class);
		if (type != null) {
			setter.apply(type);
		}
	}

	private void setConstructionAge(Function<ConstructionAge, ?> setter) {
		var age = getEnum("constructionAge", ConstructionAge.class);
		if (age != null) {
			setter.apply(age);
		}
	}

	private <T extends Enum<T>> T getEnum(String key, Class<T> type) {
		var value = properties.get(key);
		if (type.isInstance(value)) {
			return type.cast(value);
		}
		if (value instanceof String s) {
			try {
				return Enum.valueOf(type, s);
			} catch (IllegalArgumentException e) {
				return null;
			}
		}
		return null;
	}

}
