package com.greendelta.bioheating.io.citygml;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import com.greendelta.bioheating.citygml.GmlBuilding;
import com.greendelta.bioheating.citygml.GmlModel;
import com.greendelta.bioheating.io.CrsId;
import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.GeoMap;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.predict.BoostPredictor;
import com.greendelta.bioheating.util.Res;
import com.greendelta.bioheating.util.Strings;

public class CityGmlImport implements Callable<Res<Project>> {

	private final Database db;
	private final Project project;
	private final File file;
	private final Mappings mappings;
	private final BoostPredictor booster;
	private boolean withOsmImport = false;

	public CityGmlImport(
		Database db, Project project, File file
	) {
		this.db = db;
		this.project = project;
		this.file = file;
		this.mappings = Mappings.read().orElse(null);
		this.booster = BoostPredictor.getDefault();
	}

	public CityGmlImport withOsmImport(boolean b) {
		this.withOsmImport = b;
		return this;
	}

	@Override
	public Res<Project> call() {
		if (project == null)
			return Res.error("project cannot be null");
		if (file == null || !file.exists())
			return Res.error("file does not exist");
		if (mappings == null)
			return Res.error("failed to load mappings");

		var res = GmlModel.readFrom(file);
		if (res.hasError())
			return res.castError();
		var model = res.value();
		var mapRes = initMap(model);
		if (mapRes.hasError())
			return mapRes.castError();
		var map = mapRes.value();

		var items = new ArrayList<BuildingItem>(model.buildings().size());
		for (var gml : model.buildings()) {
			var item = BuildingItem.of(gml);
			if (item.isEmpty())
				continue;
			item.building().fuel(project.defaultFuel());
			map.buildings().add(item.building());
			items.add(item);
		}

		new NeighborAnalysis().run(items);
		BuildingTypes.map(items);

		try {
			var buildings = map.buildings();
			var demands = booster.predictAll(buildings);
			for (int i = 0; i < demands.length; i++) {
				buildings.get(i).heatDemand(demands[i]);
			}
		} catch (Exception e) {
			return Res.error("failed to predict heat demands", e);
		}

		if (withOsmImport) {
			var err = OsmStreetFetch.into(map);
			if (err.hasError())
				return err.wrapError("OSM import failed");
		}

		var next = project.id() == 0
			? db.insert(project)
			: db.update(project);
		return Res.of(next);
	}

	private Res<GeoMap> initMap(GmlModel model) {

		var env = model.envelope();
		if (env == null || Strings.isNil(env.srs()))
			return Res.error("no CRS defined for model");

		var crsId = CrsId.parse(env.srs()).value();

		var map = project.map();
		if (map != null) {
			return Strings.eq(crsId, map.crs())
				? Res.of(map)
				: Res.error("different CSR of model and current project map: "
				+ map.crs() + " vs. " + crsId);
		}

		map = new GeoMap().crs(crsId);
		project.map(map);
		return Res.of(map);
	}

	private Building convertBuilding(GmlBuilding b) {

		if (b == null)
			return null;
		var cs = coordinatesOf(b);
		if (cs == null)
			return null;

		int storeys = storeysOf(b, height);
		double groundArea = b.groundSurface() != null
			? b.groundSurface().getArea()
			: 0;
		double totalArea = groundArea * storeys;
		double heatedArea = heatedAreaOf(totalArea, b.function());
		double volume = volumeOf(groundArea, height, b.roofType());
		var building = new Building()

			.storeys(storeys)
			.groundArea(groundArea)
			.heatedArea(heatedArea)
			.volume(volume)
			.climateZone(climateZoneOf(b))
			.isHeated(b.address() != null)
		;

		return building;
	}



	private int storeysOf(GmlBuilding b, double height) {
		int storeys = b.storeys();
		if (storeys > 0)
			return storeys;
		var function = b.function();
		if (function == null || height == 0)
			return 1;
		var hs = mappings.defaultStoryHeight(function);
		if (hs.isEmpty())
			return 1;
		storeys = (int) Math.round(height / hs.getAsDouble());
		return Math.max(storeys, 1);
	}

	private double heatedAreaOf(double totalArea, String function) {
		var functionType = mappings.functionType(function);
		if (functionType.isPresent()) {
			var areaFactor = mappings.areaFactor(functionType.getAsInt());
			if (areaFactor.isPresent())
				return totalArea * areaFactor.getAsDouble();
		}
		return totalArea * 0.85;
	}

	private double volumeOf(
		double groundArea, double height, String roofType
	) {
		double blockVolume = groundArea * height;
		var f = mappings.roofTypeFactor(roofType).orElse(0.9);
		return blockVolume * f;
	}




}
