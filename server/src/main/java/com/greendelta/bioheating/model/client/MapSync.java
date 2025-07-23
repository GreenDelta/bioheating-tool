package com.greendelta.bioheating.model.client;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Fuel;
import com.greendelta.bioheating.model.GeoMap;
import com.greendelta.bioheating.model.Inclusion;
import com.greendelta.bioheating.model.Street;

public class MapSync {

	private final Database db;
	private final Map<Long, Building> buildings;
	private final Map<Long, Street> streets;
	private final ClientMap clientMap;

	private MapSync(Database db, GeoMap map, ClientMap clientMap) {
		this.db = db;
		this.clientMap = clientMap;
		buildings = new HashMap<>(map.buildings().size());
		for (var b : map.buildings()) {
			buildings.put(b.id(), b);
		}
		streets = new HashMap<>(map.streets().size());
		for (var s : map.streets()) {
			streets.put(s.id(), s);
		}
	}

	public static void updateFromClient(
		Database db, GeoMap map, ClientMap clientMap
	) {
		if (db != null && map != null && clientMap != null) {
			new MapSync(db, map, clientMap).sync();
		}
	}

	private void sync() {
		for (var f : clientMap.features()) {
			var props = f.properties();
			if (props == null)
				continue;
			if (!(props.get("id") instanceof Number idProp))
				continue;
			long id = idProp.longValue();
			var type = props.get("@type");
			if ("building".equals(type)) {
				syncBuilding(id, props);
			} else if ("street".equals(type)) {
				syncStreet(id, props);
			}
		}
	}

	private void syncBuilding(long id, Map<String, Object> props) {
		var b = buildings.get(id);
		if (b == null)
			return;

		syncString(props, "name", b::name);
		syncString(props, "roofType", b::roofType);
		syncString(props, "function", b::function);
		syncDouble(props, "height", b::height);
		syncInt(props, "storeys", b::storeys);
		syncDouble(props, "groundArea", b::groundArea);
		syncDouble(props, "heatedArea", b::heatedArea);
		syncDouble(props, "volume", b::volume);
		syncString(props, "country", b::country);
		syncString(props, "locality", b::locality);
		syncString(props, "postalCode", b::postalCode);
		syncString(props, "street", b::street);
		syncString(props, "streetNumber", b::streetNumber);
		syncInt(props, "climateZone", b::climateZone);
		syncDouble(props, "heatDemand", b::heatDemand);
		syncBool(props, "isHeated", b::isHeated);
		syncInclusion(props, b::inclusion);

		if (props.get("fuelId") instanceof Number num) {
			var fuel = db.getForId(Fuel.class, num.longValue());
			b.fuel(fuel);
		}

	}

	private void syncStreet(long id, Map<String, Object> props) {
		var s = streets.get(id);
		if (s == null)
			return;
		syncString(props, "name", s::name);
		syncInclusion(props, s::inclusion);
	}

	private void syncString(
		Map<String, Object> props, String key, Function<String, ?>setter) {
		var value = props.get(key);
		if (value instanceof String s) {
			setter.apply(s);
		}
	}

	private void syncDouble(
		Map<String, Object> props, String key, Function<Double, ?> setter) {
		var value = props.get(key);
		if (value instanceof Number num) {
			setter.apply(num.doubleValue());
		}
	}

	private void syncInt(
		Map<String, Object> props, String key, Function<Integer, ?> setter) {
		var value = props.get(key);
		if (value instanceof Number) {
			setter.apply(((Number) value).intValue());
		}
	}

	private void syncBool(
		Map<String, Object> props, String key, Function<Boolean, ?> setter) {
		var value = props.get(key);
		if (value instanceof Boolean b) {
			setter.apply(b);
		}
	}

	private void syncInclusion(
		Map<String, Object> props, Function<Inclusion, ?> setter) {
		var value = props.get("inclusion");
		if (value instanceof String) {
			try {
				var inclusion = Inclusion.valueOf((String) value);
				setter.apply(inclusion);
			} catch (IllegalArgumentException e) {
				// ignore
			}
		}
	}

}
