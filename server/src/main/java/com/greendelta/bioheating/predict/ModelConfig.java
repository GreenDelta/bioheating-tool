package com.greendelta.bioheating.predict;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import org.openlca.commons.Res;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;

/// The configuration of a prediction model, stored as `config.json` next to
/// the model files. It contains the linear corrections that are applied to the
/// raw model outputs. As these corrections are calibrated for a specific model,
/// the training writes a new configuration without corrections.
///
/// ```json
/// {
///   "description": "trained at 2026-11-23T10:15",
///   "corrections": {
///     "heatDemand": { "a": 1.0, "b": 0.0 },
///     "peakLoad":   { "a": 1.0, "b": 0.0 }
///   }
/// }
/// ```
///
/// @param description an optional, free description of the model
/// @param heatDemand the correction of the annual heat demand
/// @param peakLoad the correction of the peak load
public record ModelConfig(
	String description,
	Correction heatDemand,
	Correction peakLoad
) {

	public static final String FILE_NAME = "config.json";

	/// A configuration without description and corrections.
	public static final ModelConfig NONE =
		new ModelConfig(null, Correction.NONE, Correction.NONE);

	/// The configuration of a newly trained model: it has no corrections and
	/// the training time as description.
	public static ModelConfig ofTraining() {
		var time = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
		return new ModelConfig("trained at " + time, Correction.NONE, Correction.NONE);
	}

	/// The correction of the given target.
	public Correction correctionOf(Target target) {
		return switch (target) {
			case HEAT_DEMAND -> heatDemand;
			case PEAK_LOAD -> peakLoad;
		};
	}

	/// Reads the configuration from the `config.json` file in the given model
	/// directory. All correction values are required.
	public static Res<ModelConfig> read(File dir) {
		var file = new File(dir, FILE_NAME);
		if (!file.isFile()) return Res.error(
			"Model configuration not found: " + file.getAbsolutePath()
		);
		try {
			var root = new ObjectMapper().readTree(Files.readAllBytes(file.toPath()));
			var corrections = root.path("corrections");
			var heatDemand = correctionOf(corrections, "heatDemand");
			if (heatDemand.isError()) return heatDemand.wrapError(
				"Invalid model configuration " + file.getAbsolutePath()
			);
			var peakLoad = correctionOf(corrections, "peakLoad");
			if (peakLoad.isError()) return peakLoad.wrapError(
				"Invalid model configuration " + file.getAbsolutePath()
			);
			var description = root.path("description");
			return Res.ok(new ModelConfig(
				description.isString() ? description.asString() : null,
				heatDemand.value(),
				peakLoad.value()
			));
		} catch (Exception e) {
			return Res.error(
				"Failed to read model configuration: " + file.getAbsolutePath(), e
			);
		}
	}

	private static Res<Correction> correctionOf(JsonNode corrections, String key) {
		var node = corrections.path(key);
		var a = node.path("a");
		var b = node.path("b");
		if (!a.isNumber() || !b.isNumber()) return Res.error(
			"corrections." + key + " must contain the numbers `a` and `b`"
		);
		var correction = new Correction(a.asDouble(), b.asDouble());
		return Double.isFinite(correction.a()) && Double.isFinite(correction.b())
			? Res.ok(correction)
			: Res.error("corrections." + key + " contains invalid numbers");
	}

	/// Writes this configuration as `config.json` into the given directory.
	public Res<Void> write(File dir) {
		var file = new File(dir, FILE_NAME);
		try {
			var root = new LinkedHashMap<String, Object>();
			if (description != null) {
				root.put("description", description);
			}
			root.put("corrections", Map.of(
				"heatDemand", Map.of("a", heatDemand.a(), "b", heatDemand.b()),
				"peakLoad", Map.of("a", peakLoad.a(), "b", peakLoad.b())
			));
			var json = new ObjectMapper()
				.rebuild()
				.enable(SerializationFeature.INDENT_OUTPUT)
				.build()
				.writeValueAsString(root);
			Files.writeString(file.toPath(), json);
			return Res.ok();
		} catch (Exception e) {
			return Res.error(
				"Failed to write model configuration: " + file.getAbsolutePath(), e
			);
		}
	}
}
