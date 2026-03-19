package com.greendelta.bioheating.io;

import com.greendelta.bioheating.io.citygml.CityGmlImport;
import com.greendelta.bioheating.io.citygml.OsmStreetFetch;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Project;
import java.io.File;
import java.util.List;
import org.openlca.commons.Res;
import org.openlca.commons.Strings;

public class ProjectCreator {

	private final Database db;
	private final Project project;
	private final List<File> files;
	private boolean withOsmImport = true;

	public ProjectCreator(Database db, Project project, List<File> files) {
		this.db = db;
		this.project = project;
		this.files = files;
	}

	public ProjectCreator withOsmImport(boolean withOsmImport) {
		this.withOsmImport = withOsmImport;
		return this;
	}

	public Res<Project> call() {
		var init = initProject();
		if (init.isError()) return init.castError();

		var imports = importFiles();
		if (imports.isError()) return imports.castError();

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

	public Res<Project> importFiles() {
		if (files == null || files.isEmpty()) {
			return Res.error("No import files provided");
		}

		int imported = 0;
		for (var file : files) {
			if (file == null) continue;
			var res = isXlsx(file)
				? importExcelFile(file)
				: importCityGmlFile(file);
			if (res.isError()) return res;
			imported++;
		}
		return imported > 0
			? Res.ok(project)
			: Res.error("No import files provided");
	}

	public Res<Project> importCityGmlFile(File file) {
		if (file == null) {
			return Res.error("No CityGML file provided");
		}
		try {
			return new CityGmlImport(project, List.of(file)).call();
		} catch (Exception e) {
			return Res.error("project creation failed during CityGML import", e);
		}
	}

	public Res<Project> importExcelFile(File file) {
		if (file == null) {
			return Res.error("No Excel file provided");
		}
		try {
			return new XlsBuildingImport(project, file).call();
		} catch (Exception e) {
			return Res.error("project creation failed during Excel import", e);
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

	private boolean isXlsx(File file) {
		return file != null
			&& file.getName() != null
			&& file.getName().toLowerCase().endsWith(".xlsx");
	}
}
