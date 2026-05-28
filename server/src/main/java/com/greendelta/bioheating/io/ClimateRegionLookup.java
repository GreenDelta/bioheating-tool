package com.greendelta.bioheating.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.openlca.commons.Res;
import org.openlca.commons.Strings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.ClimateRegion;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Project;

public class ClimateRegionLookup {

	private static final String RESOURCE = "climate-region-data.geojson";
	private static final GeometryFactory geometryFactory = new GeometryFactory();
	private static volatile List<RegionArea> cachedAreas;

	public Res<ClimateRegion> lookup(Database db, Project project) {
		if (db == null) return Res.error("database is null");
		return lookup(project, db.getAll(ClimateRegion.class));
	}

	public Res<ClimateRegion> lookup(
		Project project,
		Iterable<ClimateRegion> climateRegions
	) {
		if (project == null) return Res.error("project is null");
		if (climateRegions == null) return Res.error("climate regions are null");

		var number = lookupRegionNumber(project);
		if (number.isError()) return number.castError();

		var byNumber = new HashMap<Integer, ClimateRegion>();
		for (var region : climateRegions) {
			if (region != null) {
				byNumber.put(region.number(), region);
			}
		}

		var region = byNumber.get(number.value());
		if (region == null) return Res.error(
			"No climate region entity found for number=" + number.value()
		);

		project.climateRegion(region);
		return Res.ok(region);
	}

	public Res<Integer> lookupRegionNumber(Project project) {
		if (project == null) return Res.error("project is null");
		if (project.map() == null) return Res.error("project map is null");
		if (project.map().buildings().isEmpty()) {
			return Res.error("project map does not contain buildings");
		}

		var building = project.map().buildings().getFirst();
		var point = representativePointOf(project.map().crs(), building);
		if (point.isError()) return point.castError();

		var areas = loadRegionAreas();
		if (areas.isError()) return areas.castError();

		for (var area : areas.value()) {
			if (area.geometry().covers(point.value())) {
				return Res.ok(area.number());
			}
		}
		return Res.error("No climate region found for first building");
	}

	static Res<List<RegionArea>> loadRegionAreas() {
		var areas = cachedAreas;
		if (areas != null) return Res.ok(areas);

		synchronized (ClimateRegionLookup.class) {
			areas = cachedAreas;
			if (areas != null) return Res.ok(areas);

			try (var stream = ClimateRegionLookup.class.getResourceAsStream(RESOURCE)) {
				if (stream == null) return Res.error(
					"Climate region resource not found: " + RESOURCE
				);

				var root = new ObjectMapper().readTree(stream);
				var features = root.path("features");
				if (!features.isArray() || features.isEmpty()) {
					return Res.error("Climate region resource does not contain features");
				}

				var loaded = new ArrayList<RegionArea>();
				for (var feature : features) {
					var number = feature.path("properties").path("climate_region").asInt(-1);
					if (number < 0) continue;

					var geometry = geometryOf(feature.path("geometry"));
					if (geometry.isError()) {
						return geometry.wrapError(
							"Failed to parse geometry of climate region=" + number
						);
					}
					loaded.add(new RegionArea(number, geometry.value()));
				}

				if (loaded.isEmpty()) {
					return Res.error("No climate region geometries loaded");
				}
				cachedAreas = List.copyOf(loaded);
				return Res.ok(cachedAreas);
			} catch (IOException e) {
				return Res.error("Failed to read climate region resource", e);
			}
		}
	}

	private Res<Point> representativePointOf(String sourceCrs, Building building) {
		if (building == null) return Res.error("building is null");
		var coordinates = building.coordinates();
		if (coordinates == null || coordinates.length < 3) {
			return Res.error("building does not contain enough coordinates");
		}

		var polygon = polygonOf(coordinates);
		if (polygon.isError()) return polygon.castError();

		var point = polygon.value().getInteriorPoint();
		if (point == null || point.isEmpty()) {
			return Res.error("Failed to derive representative point of first building");
		}

		if (Strings.equalsIgnoreCase(CrsId.wgs84().value(), sourceCrs)) {
			return Res.ok(point);
		}

		var transformer = CoordinateTransformer.toWgs84From(sourceCrs);
		if (transformer.isError()) return transformer.wrapError(
			"Failed to create coordinate transformer"
		);

		var projected = transformer.value().project(point.getX(), point.getY());
		if (projected.isError()) return projected.wrapError(
			"Failed to transform first building point to WGS84"
		);

		var xy = projected.value();
		return Res.ok(geometryFactory.createPoint(new Coordinate(xy.x, xy.y)));
	}

	private static Res<Geometry> geometryOf(JsonNode node) {
		if (node == null || node.isMissingNode()) {
			return Res.error("missing geometry node");
		}

		var type = node.path("type").asText();
		return switch (type) {
			case "Polygon" -> {
				var polygon = polygonOf(node.path("coordinates"));
				yield polygon.isError()
					? polygon.castError()
					: Res.ok(polygon.value());
			}
			case "MultiPolygon" -> multiPolygonOf(node.path("coordinates"));
			default -> Res.error("unsupported geometry type: " + type);
		};
	}

	private static Res<Polygon> polygonOf(Coordinate[] coordinates) {
		if (coordinates == null || coordinates.length < 3) {
			return Res.error("not enough polygon coordinates");
		}
		try {
			var shell = geometryFactory.createLinearRing(closeRing(coordinates));
			return Res.ok(geometryFactory.createPolygon(shell));
		} catch (Exception e) {
			return Res.error("Failed to create polygon from coordinates", e);
		}
	}

	private static Res<Polygon> polygonOf(JsonNode coordinates) {
		if (!coordinates.isArray() || coordinates.isEmpty()) {
			return Res.error("polygon coordinates missing");
		}

		try {
			var shell = ringOf(coordinates.get(0));
			if (shell.isError()) return shell.castError();

			var holes = new LinearRing[Math.max(0, coordinates.size() - 1)];
			for (int i = 1; i < coordinates.size(); i++) {
				var hole = ringOf(coordinates.get(i));
				if (hole.isError()) return hole.castError();
				holes[i - 1] = hole.value();
			}
			return Res.ok(geometryFactory.createPolygon(shell.value(), holes));
		} catch (Exception e) {
			return Res.error("Failed to create polygon geometry", e);
		}
	}

	private static Res<Geometry> multiPolygonOf(JsonNode coordinates) {
		if (!coordinates.isArray() || coordinates.isEmpty()) {
			return Res.error("multipolygon coordinates missing");
		}

		var polygons = new Polygon[coordinates.size()];
		for (int i = 0; i < coordinates.size(); i++) {
			var polygon = polygonOf(coordinates.get(i));
			if (polygon.isError()) return polygon.castError();
			polygons[i] = polygon.value();
		}
		return Res.ok(geometryFactory.createMultiPolygon(polygons));
	}

	private static Res<LinearRing> ringOf(JsonNode coordinates) {
		if (!coordinates.isArray() || coordinates.size() < 3) {
			return Res.error("linear ring coordinates missing");
		}

		var points = new Coordinate[coordinates.size()];
		for (int i = 0; i < coordinates.size(); i++) {
			var coordinate = coordinateOf(coordinates.get(i));
			if (coordinate.isError()) return coordinate.castError();
			points[i] = coordinate.value();
		}
		return Res.ok(geometryFactory.createLinearRing(closeRing(points)));
	}

	private static Res<Coordinate> coordinateOf(JsonNode node) {
		if (!node.isArray() || node.size() < 2) {
			return Res.error("invalid coordinate node");
		}
		return Res.ok(new Coordinate(node.get(0).asDouble(), node.get(1).asDouble()));
	}

	private static Coordinate[] closeRing(Coordinate[] coordinates) {
		if (coordinates.length == 0) return coordinates;

		var first = coordinates[0];
		var last = coordinates[coordinates.length - 1];
		if (Objects.equals(first, last)
			|| (first.x == last.x && first.y == last.y)) {
			return coordinates;
		}

		var closed = new Coordinate[coordinates.length + 1];
		System.arraycopy(coordinates, 0, closed, 0, coordinates.length);
		closed[closed.length - 1] = new Coordinate(first.x, first.y);
		return closed;
	}

	static record RegionArea(int number, Geometry geometry) {}
}
