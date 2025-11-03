package com.greendelta.bioheating.io.citygml;

import java.io.File;
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
	private final BoostPredictor booster;
	private boolean withOsmImport = false;

	public CityGmlImport(
		Database db, Project project, File file) {
		this.db = db;
		this.project = project;
		this.file = file;
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

		// parse the CityGML model and initialize the map
		var res = GmlModel.readFrom(file);
		if (res.hasError())
			return res.castError();
		var model = res.value();
		var mapRes = initMap(model);
		if (mapRes.hasError())
			return mapRes.castError();
		var map = mapRes.value();

		// process the building data
		var shapes = BuildingShape.allOf(model.buildings());
		if (shapes.isEmpty())
			return Res.error("No valid buildings found in CityGML model");
		var naRes = NeighborAnalysis.run(shapes);
		if (naRes.hasError())
			return naRes.wrapError("Failed to run neighbor analysis of buildings");

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
			.isHeated(b.address() != null);

		return building;
	}

}
