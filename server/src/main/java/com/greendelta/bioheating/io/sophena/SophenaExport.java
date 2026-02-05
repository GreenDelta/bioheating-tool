package com.greendelta.bioheating.io.sophena;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greendelta.bioheating.graph.NetworkTree;
import com.greendelta.bioheating.graph.NetworkTree.Junction;
import com.greendelta.bioheating.graph.NetworkTree.Segment;
import com.greendelta.bioheating.io.CoordinateTransformer;
import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.Solution;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import org.locationtech.jts.geom.GeometryFactory;
import org.openlca.commons.Res;
import org.openlca.commons.Strings;

public class SophenaExport {

	private final Solution solution;
	private final JsonNodeFactory json = JsonNodeFactory.instance;
	private final GeometryFactory geometries = new GeometryFactory();
	private final CoordinateTransformer wgs84;
	private final List<SophenaBuildingState> buildingStates;

	private SophenaExport(Solution solution, CoordinateTransformer wgs84) {
		this.solution = solution;
		this.wgs84 = wgs84;
		this.buildingStates = SophenaBuildingState.getAll();
	}

	public static Res<Void> write(Solution solution, File file) {
		if (file == null) return Res.error("No valid export file provided");
		if (
			solution == null ||
			solution.project() == null ||
			solution.project().map() == null
		) return Res.error("No valid solution provided");

		try {
			var result = CoordinateTransformer.toWgs84From(solution.project().map());
			if (result.isError()) return result.wrapError(
				"Failed to create WGS84 transformer"
			);
			var export = new SophenaExport(solution, result.value());
			var root = export.createJson();
			var mapper = new ObjectMapper();
			try (
				var fos = new FileOutputStream(file);
				var gzos = new GZIPOutputStream(fos);
				var writer = new OutputStreamWriter(gzos, StandardCharsets.UTF_8)
			) {
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

		// add the heat flow tree
		var tree = NetworkTree.of(solution);
		if (tree.isOk()) {
			var networkObj = json.objectNode();
			networkObj.put("root", tree.value().root().id());
			var junctions = json.arrayNode();
			traversalAdd(tree.value().root(), junctions);
			networkObj.set("nodes", junctions);
			obj.set("network", networkObj);
		}
		return obj;
	}

	private ObjectNode consumerOf(Building b) {
		if (b == null || !b.isHeated() || !b.isIncluded()) return null;

		var state = buildingStateOf(b);
		var loadHours =
			b.peakLoad() > 0 ? b.heatDemand() / b.peakLoad() : state.loadHours();

		var obj = json
			.objectNode()
			.put("id", consumerIdOf(b))
			.put("name", b.name())
			.put("waterFraction", 12.0)
			.put("loadHours", loadHours)
			.put("heatingLimit", 14.0);
		obj.set("location", locationOf(b));

		// building state
		var stateObj = json
			.objectNode()
			.put("id", state.id())
			.put("name", state.name());
		obj.set("buildingState", stateObj);

		var fuelObj = json
			.objectNode()
			.put("id", "031987ab-4a0d-43b3-b3e5-8f50d8e5df1e")
			.put("name", "Warmwasser");

		var consObj = json
			.objectNode()
			.put("id", UUID.randomUUID().toString())
			.put("utilisationRate", 85.73)
			.put("waterContent", 0.0)
			.put("amount", b.heatDemand() / 0.8573)
			.set("fuel", fuelObj);
		obj.set("fuelConsumptions", json.arrayNode(1).add(consObj));

		return obj;
	}

	private void traversalAdd(Junction root, ArrayNode array) {
		if (root == null) return;
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
		obj.put("id", segment.id());
		obj.put("length", segment.length());
		obj.put("target", segment.target().id());
		return obj;
	}

	private String consumerIdOf(Building building) {
		var projectId = solution.project().id();
		var cityId = building.cityId();
		var buildingId = cityId != null ? cityId : Long.toString(building.id());
		var fullId = projectId + "/" + buildingId;
		return UUID.nameUUIDFromBytes(
			fullId.getBytes(StandardCharsets.UTF_8)
		).toString();
	}

	private ObjectNode locationOf(Building b) {
		var node = json.objectNode();
		node.put("id", UUID.randomUUID().toString());
		node.put("name", b.name());
		String street = b.street();
		var num = b.streetNumber();
		if (Strings.isNotBlank(num)) {
			street = street == null ? num : street + " " + num;
		}
		node.put("street", street);
		node.put("zipCode", b.postalCode());
		node.put("city", b.locality());

		var coords = b.coordinates();
		if (coords != null && coords.length > 0) {
			try {
				var geom = geometries.createPolygon(coords);
				var centroid = geom.getCentroid();
				var res = wgs84.project(centroid.getX(), centroid.getY());
				if (res.isOk()) {
					node.put("latitude", res.value().y);
					node.put("longitude", res.value().x);
				}
			} catch (Exception ignored) {}
		}
		return node;
	}

	private SophenaBuildingState buildingStateOf(Building b) {
		var type = buildingTypeOf(b);
		double loadHours = b.peakLoad() > 0 ? b.heatDemand() / b.peakLoad() : 0;

		// best match for the given type and load hours
		if (loadHours > 0) {
			var best = SophenaBuildingState.bestMatch(
				buildingStates,
				type,
				loadHours
			);
			if (best != null) return best;
		}
		var state = SophenaBuildingState.defaultOf(buildingStates, type);
		return state != null
			? state
			: new SophenaBuildingState(
					"4e1a2929-e59a-4b1a-bb3c-dec917eb9849",
					"Standard 1979-1994",
					SophenaBuildingType.SINGLE_FAMILY_HOUSE,
					1921,
					true
				);
	}

	private SophenaBuildingType buildingTypeOf(Building b) {
		if (b == null || b.type() == null) return SophenaBuildingType.OTHER;

		// Map the internal BuildingType to SophenaBuildingType
		// Based on mapping logic similar to BuildingProcessor
		return switch (b.type()) {
			case SINGLE_FAMILY -> SophenaBuildingType.SINGLE_FAMILY_HOUSE;
			case END_TERRACE, MID_TERRACE -> SophenaBuildingType.TERRACE_HOUSE;
			case
				MULTI_FAMILY_SMALL,
				MULTI_FAMILY_MEDIUM -> SophenaBuildingType.MULTI_FAMILY_HOUSE;
			case
				MULTI_FAMILY_LARGE,
				HOUSE_GROUP -> SophenaBuildingType.BLOCK_OF_FLATS;
			case HIGH_RISE -> SophenaBuildingType.TOWER_BLOCK;
			case BUILDING_PART, OTHER -> SophenaBuildingType.OTHER;
		};
	}
}
