package com.greendelta.bioheating.io;

import com.greendelta.bioheating.model.Building;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.strtree.STRtree;
import org.openlca.commons.Strings;

/// A spatial index of the buildings of a map. It provides fast look-ups by the
/// building ID, by geometric intersection, and counts the heated neighbors of a
/// building. The Excel import uses it to match rows to existing buildings and to
/// estimate the building type from the number of neighbors.
///
/// Buildings without valid coordinates are still indexed by their ID.
public class BuildingIndex {

	/// The distance in map units up to which two buildings are considered
	/// neighbors. This is the same threshold that is used in the CityGML
	/// neighbor analysis.
	public static final double NEIGHBOR_THRESHOLD = 0.15;

	private static final GeometryFactory geometries = new GeometryFactory();

	private final Map<String, Building> byCityId = new HashMap<>();
	private final STRtree tree = new STRtree();

	private BuildingIndex() {
	}

	/// Creates an index over the given buildings. The index is built
	/// immediately, so no buildings can be added later.
	public static BuildingIndex of(Collection<Building> buildings) {
		var index = new BuildingIndex();
		if (buildings != null) {
			for (var building : buildings) {
				index.add(building);
			}
		}
		index.tree.build();
		return index;
	}

	/// Returns the building with the given city ID, or `null` when no such
	/// building is indexed.
	public Building findByCityId(String cityId) {
		return cityId == null ? null : byCityId.get(cityId);
	}

	/// Returns the building whose ground polygon has the largest intersection
	/// with the given geometry, or `null` when no building intersects it.
	public Building findIntersecting(Geometry geometry) {
		if (geometry == null || geometry.isEmpty()) return null;
		Building best = null;
		double bestArea = 0;
		for (var candidate : tree.query(geometry.getEnvelopeInternal())) {
			if (!(candidate instanceof Building building)) continue;
			var area = overlapArea(polygonOf(building), geometry);
			if (area > bestArea) {
				bestArea = area;
				best = building;
			}
		}
		return best;
	}

	/// Counts the heated neighbors of the given building within
	/// [#NEIGHBOR_THRESHOLD]. The building itself is not counted.
	public int countHeatedNeighbors(Building building) {
		return countHeatedNeighbors(polygonOf(building), building);
	}

	/// Counts the heated buildings that are within [#NEIGHBOR_THRESHOLD] of the
	/// given geometry. Use this for buildings that are not part of the index,
	/// for example new buildings from an Excel file.
	public int countHeatedNeighbors(Geometry geometry) {
		return countHeatedNeighbors(geometry, null);
	}

	/// The ground polygon of the building, or `null` when it has no valid
	/// coordinates.
	public static Polygon polygonOf(Building building) {
		if (building == null) return null;
		var coordinates = building.coordinates();
		if (coordinates == null || coordinates.length < 4) return null;
		try {
			return geometries.createPolygon(coordinates);
		} catch (Exception e) {
			return null;
		}
	}

	private void add(Building building) {
		if (building == null) return;
		var cityId = building.cityId();
		if (Strings.isNotBlank(cityId)) {
			byCityId.putIfAbsent(cityId, building);
		}
		var polygon = polygonOf(building);
		if (polygon != null) {
			tree.insert(polygon.getEnvelopeInternal(), building);
		}
	}

	private int countHeatedNeighbors(Geometry geometry, Building exclude) {
		if (geometry == null || geometry.isEmpty()) return 0;
		var query = geometry.getEnvelopeInternal().copy();
		query.expandBy(NEIGHBOR_THRESHOLD);
		int count = 0;
		for (var candidate : tree.query(query)) {
			if (candidate == exclude) continue;
			if (!(candidate instanceof Building building)) continue;
			if (!building.isHeated()) continue;
			if (isWithinDistance(geometry, polygonOf(building))) {
				count++;
			}
		}
		return count;
	}

	private static double overlapArea(Polygon polygon, Geometry geometry) {
		if (polygon == null || geometry == null) return 0;
		try {
			if (!polygon.intersects(geometry)) return 0;
			return polygon.intersection(geometry).getArea();
		} catch (Exception e) {
			return 0;
		}
	}

	private static boolean isWithinDistance(Geometry a, Geometry b) {
		if (a == null || b == null) return false;
		try {
			return a.isWithinDistance(b, NEIGHBOR_THRESHOLD);
		} catch (Exception e) {
			return false;
		}
	}
}
