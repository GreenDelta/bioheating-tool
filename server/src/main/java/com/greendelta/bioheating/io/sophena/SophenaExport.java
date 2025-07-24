package com.greendelta.bioheating.io.sophena;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.Inclusion;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.util.Res;

public class SophenaExport {

	private final JsonNodeFactory json = JsonNodeFactory.instance;
	private final DataPack pack;
	private final Project project;

	private SophenaExport(Project project, DataPack pack) {
		this.pack = pack;
		this.project = project;
	}

	public static Res<Void> write(Project project, File file) {
		try (var pack = DataPack.create(file)) {
			new SophenaExport(project, pack).run();
			return Res.VOID;
		} catch (Exception e) {
			return Res.error("failed to export project", e);
		}
	}

	private void run() throws IOException {
		var id = UUID.randomUUID().toString();
		var obj = json.objectNode()
			.put("id", id)
			.put("name", project.name())
			.put("description", project.description());
		mapRegion(obj);
		mapHeatNet(obj);

		if (project.map() == null)
			return;
		var consumers = json.arrayNode();
		for (var b : project.map().buildings()) {
			var node = mapBuilding(b);
			if (node != null) {
				consumers.add(node);
			}
		}
		obj.set("consumers", consumers);
		pack.put("projects/" + id + ".json", obj);
	}

	private void mapRegion(ObjectNode root) {
		if (project.climateRegion() == null)
			return;
		var obj = json.objectNode()
			.put("id", project.climateRegion().stationId())
			.put("name", project.climateRegion().name());
		root.set("weatherStation", obj);
	}

	private void mapHeatNet(ObjectNode root) {
		var obj = json.objectNode()
			.put("id", UUID.randomUUID().toString())
			.put("supplyTemperature", 80)
			.put("returnTemperature", 50);
		root.set("heatNet", obj);
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

		// fuel consumption
		var fuel = b.fuel();
		if (fuel == null)
			return obj;

		var fuelObj = json.objectNode()
			.put("id", fuel.refId())
			.put("name", fuel.name());

		var consObj = json.objectNode()
			.put("id", UUID.randomUUID().toString())
			.put("utilisationRate", 85.73)
			.put("waterContent", 0.0)
			.put("amount", b.heatDemand() / (fuel.calorificValue() * 0.8573))
			.set("fuel", fuelObj);
		obj.set("fuelConsumptions", json.arrayNode(1).add(consObj));

		return obj;
	}

}
