package com.greendelta.bioheating.io.citygml;

import com.greendelta.bioheating.citygml.GmlModel;
import com.greendelta.bioheating.io.CrsId;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.GeoMap;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.predict.BoostPredictor;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.openlca.commons.Res;
import org.openlca.commons.Strings;

public class CityGmlImport implements Callable<Res<Project>> {

	private final Database db;
	private final Project project;
	private final List<File> files;
	private boolean withOsmImport = false;

	public CityGmlImport(Database db, Project project, List<File> files) {
		this.db = db;
		this.project = project;
		this.files = files;
	}

	public CityGmlImport withOsmImport(boolean b) {
		this.withOsmImport = b;
		return this;
	}

	@Override
	public Res<Project> call() {
		if (project == null) return Res.error("project cannot be null");
		if (files == null || files.isEmpty()) return Res.error(
			"no CityGML files provided"
		);

		// parse all CityGML models and initialize the map
		var allShapes = new ArrayList<BuildingShape>();
		GeoMap map = null;
		for (var file : files) {
			if (file == null || !file.exists()) return Res.error(
				"file does not exist: " + file
			);
			var res = GmlModel.readFrom(file);
			if (res.isError()) return res.castError();
			var model = res.value();
			var mapRes = initMap(model);
			if (mapRes.isError()) return mapRes.castError();
			map = mapRes.value();
			allShapes.addAll(BuildingShape.allOf(model.buildings()));
		}

		if (allShapes.isEmpty()) return Res.error(
			"No valid buildings found in CityGML files"
		);

		// process the building data
		var naRes = NeighborAnalysis.run(allShapes);
		if (naRes.isError()) return naRes.wrapError(
			"Failed to run neighbor analysis of buildings"
		);
		var buildingRes = BuildingProcessor.map(allShapes);
		if (buildingRes.isError()) return buildingRes.wrapError(
			"Failed to map building data"
		);
		var buildings = buildingRes.value();
		map.buildings().addAll(buildings);

		// predict the heat demands
		var predictor = BoostPredictor.getDefault();
		if (predictor.isError()) return predictor.wrapError(
			"Failed to load the heat demand predictor"
		);
		var predictions = predictor
			.value()
			.predictAll(project.climateRegion(), buildings);
		if (predictions.isError()) return predictions.wrapError(
			"Failed to predict heat demands"
		);
		var demands = predictions.value();
		for (int i = 0; i < buildings.size(); i++) {
			var building = buildings.get(i);
			var heatDemand = demands[i];
			building.heatDemand(heatDemand);

			/// Estimates the peak heating load of the building based on its annual
			/// heat demand using a linear regression model (as in Thermos).
			/// The formula is: `peakLoad = 0.0004963 * heatDemand + 21.84`
			/// where `heatDemand` is in kWh/year and peakLoad is in kW.
			/// Note: In a future version of the model, this field will also be
			/// predicted by the machine learning model.
			building.peakLoad(0.0004963 * heatDemand + 21.84);
		}

		if (withOsmImport) {
			var err = OsmStreetFetch.into(map);
			if (err.isError()) return err.wrapError("OSM import failed");
		}

		var next = project.id() == 0 ? db.insert(project) : db.update(project);
		return Res.ok(next);
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
