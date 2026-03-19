package com.greendelta.bioheating.io;

import com.greendelta.bioheating.io.citygml.CityGmlImport;
import com.greendelta.bioheating.io.citygml.OsmStreetFetch;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Project;
import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import org.openlca.commons.Res;

public class ProjectCreator {

	private final Database db;
	private final Project project;
	private final List<File> cityGmlFiles;
	private boolean withOsmImport = true;

	public ProjectCreator(Database db, Project project, List<File> cityGmlFiles) {
		this.db = db;
		this.project = project;
		this.cityGmlFiles = cityGmlFiles;
	}

	public ProjectCreator withOsmImport(boolean withOsmImport) {
		this.withOsmImport = withOsmImport;
		return this;
	}

	public Res<Project> call() {
		var init = initProject();
		if (init.isError()) return init.castError();

		var cityGml = importCityGmlFiles();
		if (cityGml.isError()) return cityGml.castError();

		if (withOsmImport) {
			var osm = importOsm();
			if (osm.isError()) return osm.castError();
		}

		return saveProject();
	}

	public Res<Project> initProject() {
		if (db == null) return Res.error("database is null");
		if (project == null) return Res.error("project is null");
		return Res.ok(project);
	}

	public Res<Project> importCityGmlFiles() {
		if (cityGmlFiles == null || cityGmlFiles.isEmpty()) {
			return Res.error("No CityGML files provided");
		}
		try {
			return new CityGmlImport(project, cityGmlFiles).call();
		} catch (Exception e) {
			return Res.error("project creation failed during CityGML import", e);
		}
	}

	public Res<Project> importOsm() {
		if (project == null || project.map() == null) {
			return Res.error("project map is not initialized");
		}
		var osm = OsmStreetFetch.into(project.map());
		return osm.isError() ? osm.castError() : Res.ok(project);
	}

	public Res<Project> saveProject() {
		if (db == null) return Res.error("database is null");
		if (project == null) return Res.error("project is null");
		try {
			var next = project.id() == 0 ? db.insert(project) : db.update(project);
			return Res.ok(next);
		} catch (Exception e) {
			return Res.error("failed to save project", e);
		}
	}
}
