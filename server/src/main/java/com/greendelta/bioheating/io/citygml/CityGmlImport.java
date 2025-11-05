package com.greendelta.bioheating.io.citygml;

import java.io.File;
import java.util.concurrent.Callable;

import com.greendelta.bioheating.citygml.GmlModel;
import com.greendelta.bioheating.io.CrsId;
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
	private boolean withOsmImport = false;

	public CityGmlImport(
		Database db, Project project, File file) {
		this.db = db;
		this.project = project;
		this.file = file;
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
		var buildingRes = BuildingProcessor.map(shapes);
		if (buildingRes.hasError())
			return buildingRes.wrapError("Failed to map building data");
		var buildings = buildingRes.value();
		map.buildings().addAll(buildings);

		// predict the heat demands
		var predictor = BoostPredictor.getDefault();
		if (predictor.hasError())
			return predictor.wrapError("Failed to load the heat demand predictor");
		var predictions = predictor.value().predictAll(
			project.climateRegion(), buildings);
		if (predictions.hasError())
			return predictions.wrapError("Failed to predict heat demands");
		var demands = predictions.value();
		for (int i = 0; i < buildings.size(); i++) {
			buildings.get(i).heatDemand(demands[i]);
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
}
