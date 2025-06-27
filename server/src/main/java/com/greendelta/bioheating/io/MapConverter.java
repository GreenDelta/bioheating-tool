package com.greendelta.bioheating.io;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.locationtech.jts.geom.Coordinate;

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
		props.put("isIncluded", b.isIncluded());
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
		return new GeoFeature("Feature", line, props);
	}

	public record ClientMap(List<GeoFeature> features) {
	}

	public record GeoFeature(
		String type, Geometry geometry, Map<String, Object> properties
	) {
	}

	public sealed interface Geometry {

		static GeoPolygon polygonOf(Coordinate[] cs) {
			var ring = new ArrayList<List<Double>>(cs.length);
			for (var c : cs) {
				ring.add(List.of(c.x, c.y));
			}
			return new GeoPolygon("Polygon", List.of(ring));
		}

		static GeoLine lineOf(Coordinate[] cs) {
			var line = new ArrayList<List<Double>>(cs.length);
			for (var c : cs) {
				line.add(List.of(c.x, c.y));
			}
			return new GeoLine("LineString", line);
		}

		record GeoPolygon(String type, List<List<List<Double>>> coordinates)
			implements Geometry {
		}

		record GeoLine(String type, List<List<Double>> coordinates)
			implements Geometry {
		}
	}
}
