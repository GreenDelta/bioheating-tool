package com.greendelta.bioheating.io;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greendelta.bioheating.model.ClimateRegion;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Project;

public class ClimateRegionLookup {

	private static final String RESOURCE = "climate-region-data.geojson";
	private static final GeometryFactory geometryFactory = new GeometryFactory();
	private static List<RegionArea> cachedAreas;

	public ClimateRegion lookup(Database db, Project project) {
		if (db == null || project == null || project.map() == null || project.map().buildings().isEmpty()) {
			return null;
		}
		var building = project.map().buildings().getFirst();
		var coords = building.coordinates();
		if (coords == null || coords.length == 0) return null;

		double sumX = 0, sumY = 0;
		for (var pt : coords) {
			sumX += pt.x;
			sumY += pt.y;
		}
		var centroid = new Coordinate(sumX / coords.length, sumY / coords.length);

		var crs = project.map().crs();
		var wgs84 = centroid;
		if (crs != null && !crs.equalsIgnoreCase("EPSG:4326")) {
			var trans = CoordinateTransformer.toWgs84From(crs).orElse(null);
			if (trans != null) {
				var p = trans.project(centroid.x, centroid.y).orElse(null);
				if (p != null) {
					wgs84 = new Coordinate(p.x, p.y);
				}
			}
		}

		int number = lookup(wgs84);
		if (number == -1) return null;

		for (var region : db.getAll(ClimateRegion.class)) {
			if (region.number() == number) {
				project.climateRegion(region);
				return region;
			}
		}
		return null;
	}

	public int lookup(Coordinate coordinate) {
		if (coordinate == null) return -1;
		var areas = loadRegionAreas();
		var point = geometryFactory.createPoint(coordinate);

		// 1. Check intersection
		for (var area : areas) {
			if (area.geometry.covers(point)) {
				return area.number;
			}
		}

		// 2. Fallback to nearest
		double minDist = Double.MAX_VALUE;
		int bestRegion = -1;
		for (var area : areas) {
			double dist = area.geometry.distance(point);
			if (dist < minDist) {
				minDist = dist;
				bestRegion = area.number;
			}
		}
		return bestRegion;
	}

	private static synchronized List<RegionArea> loadRegionAreas() {
		if (cachedAreas != null) return cachedAreas;
		cachedAreas = new ArrayList<>();
		try (var stream = ClimateRegionLookup.class.getResourceAsStream(RESOURCE)) {
			if (stream != null) {
				var features = new ObjectMapper().readTree(stream).path("features");
				for (var feature : features) {
					int number = feature.path("properties").path("climate_region").asInt(-1);
					var geom = parseGeometry(feature.path("geometry"));
					if (number != -1 && geom != null) {
						cachedAreas.add(new RegionArea(number, geom));
					}
				}
			}
		} catch (Exception ignored) {
		}
		return cachedAreas;
	}

	private static Geometry parseGeometry(JsonNode node) {
		if (node == null || node.isMissingNode()) return null;
		String type = node.path("type").asText();
		var coords = node.path("coordinates");
		if ("Polygon".equals(type)) {
			return parsePolygon(coords);
		} else if ("MultiPolygon".equals(type)) {
			var polys = new ArrayList<Polygon>();
			for (var polyCoords : coords) {
				var p = parsePolygon(polyCoords);
				if (p != null) polys.add(p);
			}
			return geometryFactory.createMultiPolygon(polys.toArray(new Polygon[0]));
		}
		return null;
	}

	private static Polygon parsePolygon(JsonNode coords) {
		if (coords.isEmpty()) return null;
		var shell = parseRing(coords.get(0));
		if (shell == null) return null;
		var holes = new ArrayList<LinearRing>();
		for (int i = 1; i < coords.size(); i++) {
			var hole = parseRing(coords.get(i));
			if (hole != null) holes.add(hole);
		}
		return geometryFactory.createPolygon(shell, holes.toArray(new LinearRing[0]));
	}

	private static LinearRing parseRing(JsonNode coords) {
		var pts = new ArrayList<Coordinate>();
		for (var node : coords) {
			pts.add(new Coordinate(node.get(0).asDouble(), node.get(1).asDouble()));
		}
		if (pts.size() < 3) return null;
		var first = pts.get(0);
		var last = pts.get(pts.size() - 1);
		if (first.x != last.x || first.y != last.y) {
			pts.add(new Coordinate(first.x, first.y));
		}
		return geometryFactory.createLinearRing(pts.toArray(new Coordinate[0]));
	}

	private static record RegionArea(int number, Geometry geometry) {}
}
