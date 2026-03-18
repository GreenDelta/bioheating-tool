package com.greendelta.bioheating.model.client;

import com.greendelta.bioheating.io.CoordinateTransformer;
import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.GeoMap;
import com.greendelta.bioheating.model.Street;
import com.greendelta.bioheating.model.client.Geometry.GeoPolygon;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.locationtech.jts.geom.Coordinate;

/// Updates the map with the data that were sent from the client.
public class MapSync {

	private final GeoMap map;
	private final Map<Long, Building> buildings;
	private final Map<Long, Street> streets;
	private final ClientMap clientMap;
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
			if (f.isBuilding()) {
				syncBuilding(f);
			} else if (f.isStreet()) {
				syncStreet(f);
			}
		}
		// remove buildings that are not contained in the client data
		map.buildings().removeIf(
			b -> b.id() != 0 && !retainedBuildings.contains(b.id()));
	}

	private void syncBuilding(GeoFeature f) {
		long id = f.id();
		var b = buildings.get(id);
		if (b == null) {
			// new buildings will have an ID <= 0
			if (id > 0) return;
			b = createBuilding(f);
			if (b == null) return;
			map.buildings().add(b);
			buildings.put(b.id(), b);
		}
		PropertyPatch.of(f).applyOn(b);
		retainedBuildings.add(b.id());
	}

	private void syncStreet(GeoFeature f) {
		var street = streets.get(f.id());
		if (street != null) {
			PropertyPatch.of(f).applyOn(street);
		}
	}

	private Building createBuilding(GeoFeature f) {
		if (!(f.geometry() instanceof GeoPolygon polygon)) {
			return null;
		}
		var cs = coordinatesOf(polygon);
		if (cs == null || cs.length == 0) {
			return null;
		}
		var b = new Building().coordinates(cs);
		b.cityId("bh_" + UUID.randomUUID());
		return b;
	}

	private Coordinate[] coordinatesOf(GeoPolygon p) {
		var utm = CoordinateTransformer.fromWgs84To(map.crs()).orElse(null);
		if (utm == null) return null;

		if (p == null
			|| p.coordinates() == null
			|| p.coordinates().isEmpty()) {
			return null;
		}
		var ring = p.coordinates().getFirst();

		var cs = new Coordinate[ring.size()];
		for (int i = 0; i < ring.size(); i++) {
			var point = ring.get(i);
			if (point == null || point.size() < 2) {
				return null;
			}
			cs[i] = new Coordinate(point.get(0), point.get(1));
		}

		var transformed = utm.transform(cs);
		return transformed.isError()
			? null
			: closeRing(transformed.value());
	}

	private Coordinate[] closeRing(Coordinate[] cs) {
		if (cs == null || cs.length == 0) {
			return cs;
		}
		var first = cs[0];
		var last = cs[cs.length - 1];
		if (first.x == last.x && first.y == last.y) {
			return cs;
		}
		var closed = new Coordinate[cs.length + 1];
		System.arraycopy(cs, 0, closed, 0, cs.length);
		closed[closed.length - 1] = new Coordinate(first.x, first.y, first.z);
		return closed;
	}
}
