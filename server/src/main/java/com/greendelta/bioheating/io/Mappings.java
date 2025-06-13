package com.greendelta.bioheating.io;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Properties;

import com.greendelta.bioheating.util.Res;

class Mappings {

	private final Map<String, Double> defaultStoryHeights;
	private final Map<String, Integer> functionTypes;
	private final Map<String, Double> roofTypeFactors;
	private final Map<String, Integer> weatherStations;
	private final Map<Integer, Double> areaFactors;

	private Mappings(
		Map<String, Double> defaultStoryHeights,
		Map<String, Integer> functionTypes,
		Map<String, Double> roofTypeFactors,
		Map<String, Integer> weatherStations,
		Map<Integer, Double> areaFactors
	) {
		this.defaultStoryHeights = defaultStoryHeights;
		this.functionTypes = functionTypes;
		this.roofTypeFactors = roofTypeFactors;
		this.weatherStations = weatherStations;
		this.areaFactors = areaFactors;
	}

	OptionalDouble defaultStoryHeight(String function) {
		var v = defaultStoryHeights.get(function);
		return v == null
			? OptionalDouble.empty()
			: OptionalDouble.of(v);
	}

	OptionalInt functionType(String function) {
		var v = functionTypes.get(function);
		return v == null
			? OptionalInt.empty()
			: OptionalInt.of(v);
	}

	OptionalDouble roofTypeFactor(String roofType) {
		var v = roofTypeFactors.get(roofType);
		return v == null
			? OptionalDouble.empty()
			: OptionalDouble.of(v);
	}

	OptionalInt weatherStation(String municipalityKey) {
		var v = weatherStations.get(municipalityKey);
		return v == null
			? OptionalInt.empty()
			: OptionalInt.of(v);
	}

	OptionalDouble areaFactor(int type) {
		var v = areaFactors.get(type);
		return v == null
			? OptionalDouble.empty()
			: OptionalDouble.of(v);
	}

	static Res<Mappings> read() {
		var defaultStoryHeights = readDefaultStoryHeights();
		if (defaultStoryHeights.hasError())
			return defaultStoryHeights.castError();

		var functionTypes = readFunctionTypes();
		if (functionTypes.hasError())
			return functionTypes.castError();

		var roofTypeFactors = readRoofTypeFactors();
		if (roofTypeFactors.hasError())
			return roofTypeFactors.castError();

		var weatherStations = readWeatherStations();
		if (weatherStations.hasError())
			return weatherStations.castError();

		var areaFactors = readAreaFactors();
		if (areaFactors.hasError())
			return areaFactors.castError();

		return Res.of(new Mappings(
			defaultStoryHeights.value(),
			functionTypes.value(),
			roofTypeFactors.value(),
			weatherStations.value(),
			areaFactors.value()));
	}

	private static Res<Map<String, Double>> readDefaultStoryHeights() {
		var props = readProps("default_storey_heights.properties");
		if (props.hasError())
			return props.wrapError("failed to read default storey heights");
		var map = new HashMap<String, Double>();
		for (var e : props.value().entrySet()) {
			if (!(e.getKey() instanceof String key)
				|| !(e.getValue() instanceof String value))
				continue;
			try {
				map.put(key, Double.parseDouble(value));
			} catch (Exception ex) {
				return Res.error("Invalid storey height: " + value, ex);
			}
		}
		return Res.of(map);
	}

	private static Res<Map<String, Integer>> readFunctionTypes() {
		var ft = readProps("function_types.properties");
		if (ft.hasError())
			return ft.wrapError("failed to read function types");
		var functionTypes = new HashMap<String, Integer>();
		for (var e : ft.value().entrySet()) {
			if (!(e.getKey() instanceof String key)
				|| !(e.getValue() instanceof String value))
				continue;
			try {
				functionTypes.put(key, Integer.parseInt(value));
			} catch (Exception ex) {
				return Res.error("Invalid function type: " + value, ex);
			}
		}
		return Res.of(functionTypes);
	}

	private static Res<Map<String, Double>> readRoofTypeFactors() {
		var rtf = readProps("roof_type_factors.properties");
		if (rtf.hasError())
			return rtf.wrapError("failed to read roof type factors");
		var roofTypeFactors = new HashMap<String, Double>();
		for (var e : rtf.value().entrySet()) {
			if (!(e.getKey() instanceof String key)
				|| !(e.getValue() instanceof String value))
				continue;
			try {
				roofTypeFactors.put(key, Double.parseDouble(value));
			} catch (Exception ex) {
				return Res.error("Invalid roof type factor: " + value, ex);
			}
		}
		return Res.of(roofTypeFactors);
	}

	private static Res<Map<String, Integer>> readWeatherStations() {
		var ws = readProps("weather_stations.properties");
		if (ws.hasError())
			return ws.wrapError("failed to read weather stations");
		var weatherStations = new HashMap<String, Integer>();
		for (var e : ws.value().entrySet()) {
			if (!(e.getKey() instanceof String key)
				|| !(e.getValue() instanceof String value))
				continue;
			try {
				weatherStations.put(key, Integer.parseInt(value));
			} catch (Exception ex) {
				return Res.error("Invalid weather station: " + value, ex);
			}
		}
		return Res.of(weatherStations);
	}

	private static Res<Map<Integer, Double>> readAreaFactors() {
		var af = readProps("area_factors.properties");
		if (af.hasError())
			return af.wrapError("failed to read area factors");
		var areaFactors = new HashMap<Integer, Double>();
		for (var e : af.value().entrySet()) {
			if (!(e.getKey() instanceof String key)
				|| !(e.getValue() instanceof String value))
				continue;
			try {
				areaFactors.put(Integer.parseInt(key), Double.parseDouble(value));
			} catch (Exception ex) {
				return Res.error("Invalid area factor: " + value, ex);
			}
		}
		return Res.of(areaFactors);
	}

	private static Res<Properties> readProps(String resource) {
		var stream = Mappings.class.getResourceAsStream(resource);
		if (stream == null)
			return Res.error("Resource not found: " + resource);
		try (stream) {
			var props = new Properties();
			props.load(stream);
			return Res.of(props);
		} catch (Exception e) {
			return Res.error("Failed to read resource: " + resource, e);
		}
	}

}
