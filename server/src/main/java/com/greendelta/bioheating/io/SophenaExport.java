package com.greendelta.bioheating.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import org.openlca.commons.Res;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greendelta.bioheating.graph.NetworkTree;
import com.greendelta.bioheating.graph.NetworkTree.Junction;
import com.greendelta.bioheating.graph.NetworkTree.Segment;
import com.greendelta.bioheating.model.Building;
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

		// add the heat flow tree as a flat structure
		var tree = NetworkTree.of(solution);
		if (tree.isOk()) {
			var junctions = json.arrayNode();
			traversalAdd(tree.value().root(), junctions);
			obj.set("network", junctions);
		}
		return obj;
	}

	private ObjectNode consumerOf(Building b) {
		if (b == null || !b.isHeated() || !b.isIncluded())
			return null;

		var obj = json.objectNode()
			.put("id", consumerIdOf(b))
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

	private void traversalAdd(Junction root, ArrayNode array) {
		if (root == null)
			return;
		var queue = new ArrayDeque<Junction>();
		queue.add(root);
		while (!queue.isEmpty()) {
			var next = queue.poll();
			var obj = junctionOf(next);
			array.add(obj);
			for (var s : next.segments()) {
				queue.add(s.target());
			}
		}
	}

	private ObjectNode junctionOf(Junction junction) {
		var obj = json.objectNode();
		obj.put("node", junction.id());

		var b = junction.building();
		if (b != null && b.isHeated() && b.isIncluded()) {
			obj.put("consumerId", consumerIdOf(b));
			obj.put("heatDemand", b.heatDemand());
			obj.put("peakLoad", b.peakLoad());
		}

		var segments = junction.segments();
		if (!segments.isEmpty()) {
			var array = json.arrayNode();
			for (var seg : segments) {
				array.add(segmentOf(seg));
			}
			obj.set("segments", array);
		}
		return obj;
	}

	private ObjectNode segmentOf(Segment segment) {
		var obj = json.objectNode();
		obj.put("length", segment.length());
		obj.put("target", segment.target().id());
		return obj;
	}

	private String consumerIdOf(Building building) {
		var projectId = solution.project().id();
		var cityId = building.cityId();
		var buildingId = cityId != null
			? cityId
			: Long.toString(building.id());
		var fullId = projectId + "/" + buildingId;
		return UUID.nameUUIDFromBytes(
			fullId.getBytes(StandardCharsets.UTF_8)).toString();
	}
}
