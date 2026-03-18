package com.greendelta.bioheating.model.client;

import com.greendelta.bioheating.io.CoordinateTransformer;
import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.BuildingType;
import com.greendelta.bioheating.model.ConstructionAge;
import com.greendelta.bioheating.model.GeoMap;
import com.greendelta.bioheating.model.Street;
import com.greendelta.bioheating.model.client.Geometry.GeoPolygon;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.locationtech.jts.geom.Coordinate;

/// Updates the map with the data that were sent from the client.
public class MapSync {

	private final GeoMap map;
	private final Map<Long, Building> buildings;
	private final Map<Long, Street> streets;
	private final ClientMap clientMap;

	/// Contains the IDs of buildings that were updated or newly created.
	/// Buildings with IDs not in this set are removed from the map.
	private final Set<Long> retainedBuildings = new HashSet<>();

	private MapSync(GeoMap map, ClientMap clientMap) {
		this.map = map;
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

	public static void updateFromClient(GeoMap map, ClientMap clientMap) {
		if (map != null && clientMap != null) {
			new MapSync(map, clientMap).sync();
		}
	}

	private void sync() {
		for (var f : clientMap.features()) {
			var props = f.properties();
			if (props == null) continue;
			if (!(props.get("id") instanceof Number idProp)) continue;
			long id = idProp.longValue();
			var type = props.get("@type");
			if ("building".equals(type)) {
				syncBuilding(id, f, props);
			} else if ("street".equals(type)) {
				syncStreet(id, props);
			}
		}
		map.buildings().removeIf(b -> !retainedBuildings.contains(b.id()));
	}

	private void syncBuilding(
		long id, GeoFeature feature, Map<String, Object> props
	) {
		var b = buildings.get(id);
		if (b == null) {
			if (id > 0) return;
			b = createBuilding(feature, props);
			if (b == null) return;
			map.buildings().add(b);
			buildings.put(b.id(), b);
		}
		retainedBuildings.add(b.id());

		syncString(props, "name", b::name);
		syncString(props, "roofTypeCode", b::roofTypeCode);
		syncString(props, "roofTypeLabel", b::roofTypeLabel);
		syncString(props, "functionCode", b::functionCode);
		syncString(props, "functionLabel", b::functionLabel);
		syncBuildingType(props, b::type);
		syncConstructionAge(props, b::constructionAge);
		syncDouble(props, "height", b::height);
		syncInt(props, "storeys", b::storeys);
		syncDouble(props, "groundArea", b::groundArea);
		syncString(props, "country", b::country);
		syncString(props, "locality", b::locality);
		syncString(props, "postalCode", b::postalCode);
		syncString(props, "street", b::street);
		syncString(props, "streetNumber", b::streetNumber);
		syncDouble(props, "heatDemand", b::heatDemand);
		syncDouble(props, "peakLoad", b::peakLoad);
		syncBool(props, "isHeated", b::isHeated);
		syncBool(props, "isSupplyCenter", b::isSupplyCenter);
		syncBool(props, "isIncluded", b::isIncluded);
	}

	private void syncStreet(long id, Map<String, Object> props) {
		var s = streets.get(id);
		if (s == null) return;
		syncString(props, "name", s::name);
		syncBool(props, "isExcluded", s::isExcluded);
	}

	private void syncString(
		Map<String, Object> props,
		String key,
		Function<String, ?> setter
	) {
		var value = props.get(key);
		if (value instanceof String s) {
			setter.apply(s);
		}
	}

	private void syncDouble(
		Map<String, Object> props,
		String key,
		Function<Double, ?> setter
	) {
		var value = props.get(key);
		if (value instanceof Number num) {
			setter.apply(num.doubleValue());
		}
	}

	private void syncInt(
		Map<String, Object> props,
		String key,
		Function<Integer, ?> setter
	) {
		var value = props.get(key);
		if (value instanceof Number) {
			setter.apply(((Number) value).intValue());
		}
	}

	private void syncBool(
		Map<String, Object> props,
		String key,
		Function<Boolean, ?> setter
	) {
		var value = props.get(key);
		if (value instanceof Boolean b) {
			setter.apply(b);
		}
	}

	private void syncBuildingType(
		Map<String, Object> props,
		Function<BuildingType, ?> setter
	) {
		var value = props.get("type");
		if (value instanceof String) {
			try {
				var type = BuildingType.valueOf((String) value);
				setter.apply(type);
			} catch (IllegalArgumentException e) {
				// ignore, use default
			}
		}
	}

	private void syncConstructionAge(
		Map<String, Object> props,
		Function<ConstructionAge, ?> setter
	) {
		var value = props.get("constructionAge");
		if (value instanceof String) {
			try {
				var age = ConstructionAge.valueOf((String) value);
				setter.apply(age);
			} catch (IllegalArgumentException e) {
				// ignore, use default
			}
		}
	}

	private Building createBuilding(
		GeoFeature feature, Map<String, Object> props
	) {
		if (feature == null
			|| !(feature.geometry() instanceof GeoPolygon polygon)) {
			return null;
		}
		var coordinates = coordinatesOf(polygon);
		if (coordinates == null || coordinates.length == 0) {
			return null;
		}
		var b = new Building().coordinates(coordinates);
		var cityId = props.get("id");
		if (cityId != null) {
			b.cityId("client:" + cityId);
		}
		return b;
	}

	private Coordinate[] coordinatesOf(GeoPolygon polygon) {
		if (polygon == null || polygon.coordinates() == null) {
			return null;
		}
		List<List<Double>> ring = null;
		for (var candidate : polygon.coordinates()) {
			if (candidate != null && !candidate.isEmpty()) {
				ring = candidate;
				break;
			}
		}
		if (ring == null) {
			return null;
		}

		var wgs84ToMap = CoordinateTransformer.fromWgs84To(map.crs()).orElse(null);
		if (wgs84ToMap == null) {
			return null;
		}

		var wgs84 = new Coordinate[ring.size()];
		for (int i = 0; i < ring.size(); i++) {
			var point = ring.get(i);
			if (point == null || point.size() < 2) {
				return null;
			}
			wgs84[i] = new Coordinate(point.get(0), point.get(1));
		}

		var transformed = wgs84ToMap.transform(wgs84);
		return transformed.isError() ? null : closeRing(transformed.value());
	}

	private Coordinate[] closeRing(Coordinate[] coordinates) {
		if (coordinates == null || coordinates.length == 0) {
			return coordinates;
		}
		var first = coordinates[0];
		var last = coordinates[coordinates.length - 1];
		if (Objects.equals(first, last)
			|| (first.x == last.x && first.y == last.y && first.z == last.z)) {
			return coordinates;
		}
		var closed = new Coordinate[coordinates.length + 1];
		System.arraycopy(coordinates, 0, closed, 0, coordinates.length);
		closed[closed.length - 1] = new Coordinate(first.x, first.y, first.z);
		return closed;
	}
}
