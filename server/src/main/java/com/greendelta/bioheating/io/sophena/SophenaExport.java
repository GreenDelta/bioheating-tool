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
import com.greendelta.bioheating.model.Project;

public class SophenaExport {

	private final JsonNodeFactory json = JsonNodeFactory.instance;
	private final Project project;

	private SophenaExport(Project project) {
		this.project = project;
	}

	public static Res<Void> write(Project project, File file) {
		try {
			var export = new SophenaExport(project);
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
			return Res.error("failed to export project", e);
		}
	}

	private ObjectNode createJson() {
		var obj = json.objectNode();
		var consumers = json.arrayNode();

		if (project.map() != null) {
			for (var b : project.map().buildings()) {
				var node = mapBuilding(b);
				if (node != null) {
					consumers.add(node);
				}
			}
		}

		obj.set("consumers", consumers);
		return obj;
	}

	private ObjectNode mapBuilding(Building b) {
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
