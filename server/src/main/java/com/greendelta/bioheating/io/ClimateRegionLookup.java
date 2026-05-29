package com.greendelta.bioheating.io;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.openlca.commons.Res;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greendelta.bioheating.model.ClimateRegion;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Project;

public class ClimateRegionLookup {

	private final GeometryFactory factory = new GeometryFactory();

	private ClimateRegionLookup() {
	}

	public static Res<ClimateRegion> lookup(Database db, Project project) {
		if (db == null)
			return Res.error("No database provided");
		if (project == null
			|| project.map() == null
			|| project.map().crs() == null
			|| project.map().buildings().isEmpty()) {
			return Res.error("Project has no valid building data");
		}

		// find the first best coordinate
		Coordinate cx = null;
		for (var b : project.map().buildings()) {
			var cs = b.coordinates();
			if (cs != null && cs.length > 0) {
				cx = cs[0];
				break;
			}
		}
		if (cx == null)
			return Res.error("No building with coordinates found in project");

		// project the coordinate to WGS 84
		var crs = project.map().crs();
		var trans = CoordinateTransformer.toWgs84From(crs);
		if (trans.isError()) {
			return trans.wrapError("Failed to get WGS 84 transformer for CRS: " + crs);
		}
		var projected = trans.value().project(cx.x, cx.y);
		if (projected.isError()) {
			return projected.wrapError("Failed to project coordinate for CRS: " + crs);
		}

		// lookup the region code
		var lookupPoint = new Coordinate(
			projected.value().x, projected.value().y);
		int number = lookup(lookupPoint);
		if (number == -1)
			return Res.error("Lookup failed for point: " + lookupPoint);

		// find the region for the code
		for (var region : db.getAll(ClimateRegion.class)) {
			if (region.number() == number)
				return Res.ok(region);
		}
		return Res.error("No region found with region code: " + number);
	}

	public static int lookup(Coordinate cx) {
		if (cx == null) return -1;
		try {
			return new ClimateRegionLookup().of(cx);
		} catch (Exception ignore) {
			return -1;
		}
	}

	private int of(Coordinate cx) {
		var areas = loadAreas();
		if (areas.isEmpty())
			return -1;

		var point = factory.createPoint(cx);

		// check intersection
		for (var area : areas) {
			if (area.geometry.covers(point)) {
				return area.number;
			}
		}

		// fallback to nearest
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

	private List<RegionArea> loadAreas() {
		var stream = getClass().getResourceAsStream("climate-region-data.geojson");
		if (stream == null)
			return List.of();

		try (stream) {
			var features = new ObjectMapper().readTree(stream).path("features");
			var areas = new ArrayList<RegionArea>();
			for (var f : features) {
				int number = f.path("properties")
					.path("climate_region")
					.asInt(-1);
				if (number == -1)
					continue;
				for (var p : polygonsOf(f.path("geometry"))) {
					areas.add(new RegionArea(number, p));
				}
			}
			return areas;
		} catch (Exception ignored) {
			return List.of();
		}
	}

	private List<Polygon> polygonsOf(JsonNode node) {
		if (node == null || node.isMissingNode())
			return null;
		var type = node.path("type").asText();
		var coords = node.path("coordinates");
		if ("Polygon".equals(type)) {
			var p = polygonOf(coords);
			return p != null ? List.of(p) : List.of();
		} else if ("MultiPolygon".equals(type)) {
			var polys = new ArrayList<Polygon>();
			for (var polyCoords : coords) {
				var p = polygonOf(polyCoords);
				if (p != null) {
					polys.add(p);
				}
			}
			return polys;
		}
		return List.of();
	}

	private Polygon polygonOf(JsonNode coords) {
		if (coords.isEmpty())
			return null;
		var shell = ringOf(coords.get(0));
		// we can ignore holes for climate regions
		return shell != null
			? factory.createPolygon(shell)
			: null;
	}

	private LinearRing ringOf(JsonNode coords) {
		var pts = new ArrayList<Coordinate>();
		for (var node : coords) {
			double x = node.get(0).asDouble();
			double y = node.get(1).asDouble();
			pts.add(new Coordinate(x, y));
		}
		if (pts.size() < 3)
			return null;

		// make sure the ring is closed
		var first = pts.getFirst();
		var last = pts.getLast();
		if (first.x != last.x || first.y != last.y) {
			pts.add(new Coordinate(first.x, first.y));
		}
		return factory.createLinearRing(pts.toArray(new Coordinate[0]));
	}

	private record RegionArea(int number, Geometry geometry) {}
}
