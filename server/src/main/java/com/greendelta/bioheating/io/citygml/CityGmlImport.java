package com.greendelta.bioheating.io.citygml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.jspecify.annotations.NullMarked;
import org.openlca.commons.Res;
import org.openlca.commons.Strings;

import com.greendelta.bioheating.citygml.GmlModel;
import com.greendelta.bioheating.io.ClimateRegionLookup;
import com.greendelta.bioheating.io.CrsId;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.GeoMap;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.predict.BoostPredictor;

@NullMarked
public class CityGmlImport implements Callable<Res<Project>> {

	private final Database db;
	private final Project project;
	private final List<File> files;
	private final BoostPredictor predictor;

	public CityGmlImport(
		Database db,
		Project project,
		List<File> files,
		BoostPredictor predictor
	) {
		this.db = db;
		this.project = project;
		this.files = files;
		this.predictor = predictor;
	}

	@Override
	public Res<Project> call() {
		if (files.isEmpty()) {
			return Res.error("No CityGML files provided");
		}

		// parse all CityGML models and initialize the map
		var shapes = new ArrayList<BuildingShape>();
		GeoMap map = null;
		for (var file : files) {
			var res = GmlModel.readFrom(file);
			if (res.isError()) {
				return res.castError();
			}
			var model = res.value();
			shapes.addAll(BuildingShape.allOf(model.buildings()));

			// we check the CRS of each file
			var mapRes = initMap(model);
			if (mapRes.isError()) {
				return mapRes.castError();
			}
			if (map == null) {
				map = mapRes.value();
			}
		}
		if (shapes.isEmpty())
			return Res.error("No valid buildings found in CityGML files");


		// process the building data
		var naRes = NeighborAnalysis.run(shapes);
		if (naRes.isError()) {
			return naRes.wrapError("Failed to run neighbor analysis of buildings");
		}
		var buildingRes = BuildingProcessor.map(shapes);
		if (buildingRes.isError()) return buildingRes.wrapError(
			"Failed to map building data"
		);
		var buildings = buildingRes.value();
		map.buildings().addAll(buildings);

		// important to try to determine the climate region before the demand
		// prediction
		if (project.climateRegion() == null) {
			var region = ClimateRegionLookup.lookup(db, map);
			if (region.isError())
				return region.wrapError("Failed to determine climate region");
			project.climateRegion(region.value());
		}

		// predict the heat demands
		var predictions = predictor
			.predictAll(project.climateRegion(), buildings);
		if (predictions.isError()) return predictions.wrapError(
			"Failed to predict heat demands"
		);
		var predictionsByBuilding = predictions.value();
		for (int i = 0; i < buildings.size(); i++) {
			var building = buildings.get(i);
			var prediction = predictionsByBuilding.get(i);
			building.heatDemand(prediction.heatDemand());
			building.peakLoad(prediction.peakLoad());
		}

		return Res.ok(project);
	}

	private Res<GeoMap> initMap(GmlModel model) {
		var env = model.envelope();
		if (env == null || Strings.isBlank(env.srs())) return Res.error(
			"no CRS defined for model"
		);

		var crsId = CrsId.parse(env.srs()).value();

		var map = project.map();
		if (map != null) {
			return Strings.equalsIgnoreCase(crsId, map.crs())
				? Res.ok(map)
				: Res.error(
				"different CSR of model and current project map: " +
					map.crs() +
					" vs. " +
					crsId
			);
		}

		map = new GeoMap().crs(crsId);
		project.map(map);
		return Res.ok(map);
	}
}
