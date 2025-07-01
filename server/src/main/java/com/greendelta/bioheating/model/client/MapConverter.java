package com.greendelta.bioheating.model.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.greendelta.bioheating.io.CoordinateTransformer;
import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.GeoMap;
import com.greendelta.bioheating.model.Street;
import com.greendelta.bioheating.util.Res;

public class MapConverter {

	private final CoordinateTransformer transformer;
	private final GeoMap map;

	private MapConverter(CoordinateTransformer transformer, GeoMap map) {
		this.transformer = Objects.requireNonNull(transformer);
		this.map = Objects.requireNonNull(map);
	}

	public static Res<ClientMap> toClient(GeoMap map) {
		var res = CoordinateTransformer.toWgs84From(map);
		if (res.hasError())
			return res.wrapError("failed to create transformer for map CRS");
		var clientMap = new MapConverter(res.value(), map).convert();
		return Res.of(clientMap);
	}

	private ClientMap convert() {
		var features = new ArrayList<GeoFeature>(
			map.buildings().size() + map.streets().size());

		// buildings as polygon features
		for (var b : map.buildings()) {
			var feature = convert(b);
			if (feature != null) {
				features.add(feature);
			}
		}

		// streets as line-string features
		for (var s : map.streets()) {
			var feature = convert(s);
			if (feature != null) {
				features.add(feature);
			}
		}

		return new ClientMap(features);
	}

	private GeoFeature convert(Building b) {
		if (b == null)
			return null;
		var cs = transformer.transform(b.coordinates());
		if (cs.hasError())
			return null;
		var polygon = Geometry.polygonOf(cs.value());
		var props = new HashMap<String, Object>();
		props.put("@type", "building");
		props.put("id", b.id());
		props.put("name", b.name());
		props.put("roofType", b.roofType());
		props.put("function", b.function());
		props.put("height", b.height());
		props.put("storeys", b.storeys());
		props.put("groundArea", b.groundArea());
		props.put("heatedArea", b.heatedArea());
		props.put("volume", b.volume());
		props.put("country", b.country());
		props.put("locality", b.locality());
		props.put("postalCode", b.postalCode());
		props.put("street", b.street());
		props.put("streetNumber", b.streetNumber());
		props.put("climateZone", b.climateZone());
		props.put("heatDemand", b.heatDemand());
		props.put("isHeated", b.isHeated());
		props.put("inclusion", b.inclusion());
		return new GeoFeature("Feature", polygon, props);
	}

	private GeoFeature convert(Street s) {
		if (s == null)
			return null;
		var cs = transformer.transform(s.coordinates());
		if (cs.hasError())
			return null;
		var line = Geometry.lineOf(cs.value());
		var props = new HashMap<String, Object>();
		props.put("@type", "street");
		props.put("id", s.id());
		props.put("name", s.name());
		props.put("inclusion", s.inclusion());
		return new GeoFeature("Feature", line, props);
	}

	public static Res<Void> updateFromClient(GeoMap map, ClientMap clientMap) {
		if (map == null || clientMap == null)
			return Res.error("map or client map is null");

		for (var feature : clientMap.features()) {
			var props = feature.properties();
			if (props == null)
				continue;

			var type = props.get("@type");
			var id = props.get("id");
			if (!(id instanceof Number))
				continue;

			long entityId = ((Number) id).longValue();

			if ("building".equals(type)) {
				updateBuilding(map, entityId, props);
			} else if ("street".equals(type)) {
				updateStreet(map, entityId, props);
			}
		}

		return Res.VOID;
	}

	private static void updateBuilding(GeoMap map, long id, Map<String, Object> props) {
		var building = map.buildings().stream()
			.filter(b -> b.id() == id)
			.findFirst()
			.orElse(null);
		if (building == null)
			return;

		// Update building properties (coordinates never change)
		updateStringProperty(props, "name", building::name);
		updateStringProperty(props, "roofType", building::roofType);
		updateStringProperty(props, "function", building::function);
		updateDoubleProperty(props, "height", building::height);
		updateIntProperty(props, "storeys", building::storeys);
		updateDoubleProperty(props, "groundArea", building::groundArea);
		updateDoubleProperty(props, "heatedArea", building::heatedArea);
		updateDoubleProperty(props, "volume", building::volume);
		updateStringProperty(props, "country", building::country);
		updateStringProperty(props, "locality", building::locality);
		updateStringProperty(props, "postalCode", building::postalCode);
		updateStringProperty(props, "street", building::street);
		updateStringProperty(props, "streetNumber", building::streetNumber);
		updateIntProperty(props, "climateZone", building::climateZone);
		updateDoubleProperty(props, "heatDemand", building::heatDemand);
		updateBooleanProperty(props, "isHeated", building::isHeated);
		updateInclusionProperty(props, "inclusion", building::inclusion);
	}

	private static void updateStreet(GeoMap map, long id, Map<String, Object> props) {
		var street = map.streets().stream()
			.filter(s -> s.id() == id)
			.findFirst()
			.orElse(null);
		if (street == null)
			return;

		// Update street properties (coordinates never change)
		updateStringProperty(props, "name", street::name);
		updateInclusionProperty(props, "inclusion", street::inclusion);
	}

	private static void updateStringProperty(Map<String, Object> props, String key,
			java.util.function.Function<String, ?> setter) {
		var value = props.get(key);
		if (value instanceof String) {
			setter.apply((String) value);
		}
	}

	private static void updateDoubleProperty(Map<String, Object> props, String key,
			java.util.function.Function<Double, ?> setter) {
		var value = props.get(key);
		if (value instanceof Number) {
			setter.apply(((Number) value).doubleValue());
		}
	}

	private static void updateIntProperty(Map<String, Object> props, String key,
			java.util.function.Function<Integer, ?> setter) {
		var value = props.get(key);
		if (value instanceof Number) {
			setter.apply(((Number) value).intValue());
		}
	}

	private static void updateBooleanProperty(Map<String, Object> props, String key,
			java.util.function.Function<Boolean, ?> setter) {
		var value = props.get(key);
		if (value instanceof Boolean) {
			setter.apply((Boolean) value);
		}
	}

	private static void updateInclusionProperty(Map<String, Object> props, String key,
			java.util.function.Function<com.greendelta.bioheating.model.Inclusion, ?> setter) {
		var value = props.get(key);
		if (value instanceof String) {
			try {
				var inclusion = com.greendelta.bioheating.model.Inclusion.valueOf((String) value);
				setter.apply(inclusion);
			} catch (IllegalArgumentException e) {
				// Invalid inclusion value, ignore
			}
		}
	}

}
