package com.greendelta.bioheating.io.sophena;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import org.openlca.commons.Res;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.Inclusion;
import com.greendelta.bioheating.model.Solution;

public class SophenaExport {

	private final JsonNodeFactory json = JsonNodeFactory.instance;
	private final Solution solution;

	private SophenaExport(Solution solution) {
		this.solution = solution;
	}

	public static Res<Void> write(Solution solution, File file) {
		if (file == null)
			return Res.error("No valid export file provided");
		if (solution == null
			|| solution.project() == null
			|| solution.project().map() == null)
			return Res.error("No valid solution provided");

		try {
			var export = new SophenaExport(solution);
			var root = export.createJson();
			var mapper = new ObjectMapper();
			try (var fos = new FileOutputStream(file);
					 var gzos = new GZIPOutputStream(fos);
					 var writer = new OutputStreamWriter(gzos, StandardCharsets.UTF_8)) {
				mapper
					.enable(SerializationFeature.INDENT_OUTPUT)
					.writeValue(writer, root);
			}
			return Res.ok();
		} catch (Exception e) {
			return Res.error("Failed to export project", e);
		}
	}

	private ObjectNode createJson() {
		var obj = json.objectNode();
		var consumers = json.arrayNode();
		var buildings = solution.project().map().buildings();
		for (var b : buildings) {
			var node = consumerOf(b);
			if (node != null) {
				consumers.add(node);
			}
		}
		obj.set("consumers", consumers);
		return obj;
	}

	private ObjectNode consumerOf(Building b) {
		if (b == null || !b.isHeated() || b.inclusion() != Inclusion.REQUIRED)
			return null;

		var obj = json.objectNode()
			.put("id", UUID.randomUUID().toString())
			.put("name", b.name())
			.put("waterFraction", 12.0)
			.put("loadHours", 1921)
			.put("heatingLimit", 14.0);

		// building state
		var stateObj = json.objectNode()
			.put("id", "4e1a2929-e59a-4b1a-bb3c-dec917eb9849")
			.put("name", "Standard 1979-1994");
		obj.set("buildingState", stateObj);

		var fuelObj = json.objectNode()
			.put("id", "031987ab-4a0d-43b3-b3e5-8f50d8e5df1e")
			.put("name", "Warmwasser");

		var consObj = json.objectNode()
			.put("id", UUID.randomUUID().toString())
			.put("utilisationRate", 85.73)
			.put("waterContent", 0.0)
			.put("amount", b.heatDemand() / 0.8573)
			.set("fuel", fuelObj);
		obj.set("fuelConsumptions", json.arrayNode(1).add(consObj));

		return obj;
	}

}
