package com.greendelta.bioheating.io;

import com.greendelta.bioheating.io.citygml.OsmStreetFetch;
import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.predict.BoostPredictor;

import java.io.File;
import java.util.Set;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.openlca.commons.Res;

/// Updates the data of an existing project. The update steps can be combined
/// freely and are executed in the following order:
///
/// 1. import building data from an Excel file: matching buildings are updated
///    and new buildings are added, other buildings are not changed
/// 2. add new streets from OSM: only streets with a new OSM ID are added, so
///    that existing streets keep their settings; new buildings from the Excel
///    import extend the area in which the streets are fetched
/// 3. estimate the heat demand and the peak load of all heated buildings;
///    values that are provided in the Excel file of the same update are kept
///
/// The project is only saved when all selected steps succeed.
@NullMarked
public class ProjectUpdate {

	private final Database db;
	private final Project project;
	private final BoostPredictor predictor;

	private @Nullable File excelFile;
	private boolean withStreets;
	private boolean withEstimation;

	public ProjectUpdate(Database db, Project project, BoostPredictor predictor) {
		this.db = db;
		this.project = project;
		this.predictor = predictor;
	}

	public ProjectUpdate withExcelFile(@Nullable File file) {
		this.excelFile = file;
		return this;
	}

	public ProjectUpdate withStreets(boolean withStreets) {
		this.withStreets = withStreets;
		return this;
	}

	public ProjectUpdate withEstimation(boolean withEstimation) {
		this.withEstimation = withEstimation;
		return this;
	}

	public Res<Project> call() {
		if (excelFile == null && !withStreets && !withEstimation)
			return Res.error("No update selected");
		var res = update();
		return res.isError() ? res : save();
	}

	/// Executes the update steps without saving the project. This is
	/// package-private for testing.
	Res<Project> update() {
		Set<Building> providedDemands = Set.of();
		Set<Building> providedPeakLoads = Set.of();

		if (excelFile != null) {
			var xls = new XlsBuildingImport(db, project, excelFile, predictor);
			var res = xls.call();
			if (res.isError())
				return res.wrapError("Failed to import the Excel file");
			providedDemands = xls.providedHeatDemands();
			providedPeakLoads = xls.providedPeakLoads();
		}

		var map = project.map();
		if ((withStreets || withEstimation) && map == null)
			return Res.error("The project has no map");

		if (withStreets) {
			var res = OsmStreetFetch.into(map);
			if (res.isError())
				return res.wrapError("Failed to add new streets from OSM");
		}

		if (withEstimation) {
			var res = estimate(providedDemands, providedPeakLoads);
			if (res.isError())
				return res.castError();
		}
		return Res.ok(project);
	}

	private Res<Void> estimate(
		Set<Building> providedDemands, Set<Building> providedPeakLoads
	) {
		var map = project.map();
		var heated = map.buildings()
			.stream()
			.filter(Building::isHeated)
			.toList();
		if (heated.isEmpty())
			return Res.ok();

		var region = project.climateRegion();
		if (region == null) {
			var lookup = ClimateRegionLookup.lookup(db, map);
			if (lookup.isError())
				return lookup.wrapError("Failed to determine the climate region");
			region = lookup.value();
			project.climateRegion(region);
		}

		var predictions = predictor.predictAll(region, heated);
		if (predictions.isError())
			return predictions.wrapError(
				"Failed to estimate the heat demand and the peak load");
		var values = predictions.value();
		for (int i = 0; i < heated.size(); i++) {
			var building = heated.get(i);
			var prediction = values.get(i);
			if (!providedDemands.contains(building)) {
				building.heatDemand(prediction.heatDemand());
			}
			if (!providedPeakLoads.contains(building)) {
				building.peakLoad(prediction.peakLoad());
			}
		}
		return Res.ok();
	}

	private Res<Project> save() {
		try {
			return Res.ok(db.update(project));
		} catch (Exception e) {
			return Res.error("Failed to save the project", e);
		}
	}
}
