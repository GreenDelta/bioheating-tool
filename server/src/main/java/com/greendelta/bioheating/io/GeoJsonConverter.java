package com.greendelta.bioheating.io;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.Coordinate;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.GeoMap;
import com.greendelta.bioheating.util.Res;

/**
 * Converts GeoMap data to GeoJSON format with coordinates transformed to WGS84.
 */
public class GeoJsonConverter {

	private final Wsg84Transformer transformer;

	private GeoJsonConverter(Wsg84Transformer transformer) {
		this.transformer = transformer;
	}

	/**
	 * Creates a GeoJSON converter for the given map using its CRS.
	 */
	public static Res<GeoJsonConverter> forMap(GeoMap map) {
		var transformerResult = Wsg84Transformer.getForModel(map);
		if (transformerResult.hasError()) {
			return Res.error("Failed to create transformer: " + transformerResult.error());
		}
		return Res.of(new GeoJsonConverter(transformerResult.value()));
	}

	/**
	 * Converts a GeoMap to a GeoJSON FeatureCollection.
	 */
	public Res<Map<String, Object>> convert(GeoMap map) {
		if (map == null) {
			return Res.error("Map is null");
		}

		try {
			var featureCollection = new HashMap<String, Object>();
			featureCollection.put("type", "FeatureCollection");
			
			var features = new ArrayList<Map<String, Object>>();
			
			// Convert buildings to GeoJSON features
			for (var building : map.buildings()) {
				var featureResult = convertBuilding(building);
				if (featureResult.hasError()) {
					return Res.error("Failed to convert building: " + featureResult.error());
				}
				features.add(featureResult.value());
			}
			
			featureCollection.put("features", features);
			return Res.of(featureCollection);
			
		} catch (Exception e) {
			return Res.error("Failed to convert map to GeoJSON", e);
		}
	}

	/**
	 * Converts a Building to a GeoJSON Feature.
	 */
	private Res<Map<String, Object>> convertBuilding(Building building) {
		if (building == null) {
			return Res.error("Building is null");
		}

		try {
			var feature = new HashMap<String, Object>();
			feature.put("type", "Feature");
			
			// Add properties
			var properties = new HashMap<String, Object>();
			properties.put("id", building.id());
			properties.put("name", building.name());
			properties.put("type", "building");
			feature.put("properties", properties);
			
			// Convert geometry
			var geometryResult = convertBuildingGeometry(building.coordinates());
			if (geometryResult.hasError()) {
				return Res.error("Failed to convert building geometry: " + geometryResult.error());
			}
			feature.put("geometry", geometryResult.value());
			
			return Res.of(feature);
			
		} catch (Exception e) {
			return Res.error("Failed to convert building to GeoJSON feature", e);
		}
	}

	/**
	 * Converts building coordinates to GeoJSON Polygon geometry.
	 */
	private Res<Map<String, Object>> convertBuildingGeometry(Coordinate[] coordinates) {
		if (coordinates == null || coordinates.length == 0) {
			return Res.error("Building has no coordinates");
		}

		try {
			// Transform coordinates to WGS84
			var transformedCoords = transformer.transform(coordinates);
			if (transformedCoords == null) {
				return Res.error("Failed to transform coordinates");
			}

			var geometry = new HashMap<String, Object>();
			geometry.put("type", "Polygon");
			
			// Convert coordinates to GeoJSON format [longitude, latitude]
			var geoJsonCoords = new ArrayList<List<List<Double>>>();
			var ring = new ArrayList<List<Double>>();
			
			for (var coord : transformedCoords) {
				var point = new ArrayList<Double>();
				point.add(coord.x); // longitude
				point.add(coord.y); // latitude
				ring.add(point);
			}
			
			// Ensure the ring is closed (first and last coordinate should be the same)
			if (!ring.isEmpty()) {
				var first = ring.get(0);
				var last = ring.get(ring.size() - 1);
				if (!first.equals(last)) {
					ring.add(new ArrayList<>(first));
				}
			}
			
			geoJsonCoords.add(ring);
			geometry.put("coordinates", geoJsonCoords);
			
			return Res.of(geometry);
			
		} catch (Exception e) {
			return Res.error("Failed to convert coordinates to GeoJSON", e);
		}
	}

	/**
	 * Converts coordinates to a GeoJSON Point geometry.
	 * This can be used for future features like building centroids.
	 */
	public Res<Map<String, Object>> convertToPoint(Coordinate coordinate) {
		if (coordinate == null) {
			return Res.error("Coordinate is null");
		}

		try {
			var coords = new Coordinate[] { coordinate };
			var transformedCoords = transformer.transform(coords);
			if (transformedCoords == null || transformedCoords.length == 0) {
				return Res.error("Failed to transform coordinate");
			}

			var geometry = new HashMap<String, Object>();
			geometry.put("type", "Point");
			
			var geoJsonCoord = new ArrayList<Double>();
			geoJsonCoord.add(transformedCoords[0].x); // longitude
			geoJsonCoord.add(transformedCoords[0].y); // latitude
			
			geometry.put("coordinates", geoJsonCoord);
			
			return Res.of(geometry);
			
		} catch (Exception e) {
			return Res.error("Failed to convert coordinate to GeoJSON point", e);
		}
	}
}
