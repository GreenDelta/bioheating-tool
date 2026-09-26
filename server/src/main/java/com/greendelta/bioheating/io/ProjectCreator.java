package com.greendelta.bioheating.io;

import com.greendelta.bioheating.io.citygml.CityGmlImport;
import com.greendelta.bioheating.io.citygml.OsmStreetFetch;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Project;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.openlca.commons.Res;

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
		if (init.isError())
			return init;

		var imports = importFiles();
		if (imports.isError())
			return imports;

		var region = determineClimateRegion();
		if (region.isError())
			return region;

		if (withOsmImport) {
			var osm = importOsm();
			if (osm.isError())
				return osm;
		}

		return saveProject();
	}

	private Res<Project> initProject() {
		if (db == null)
			return Res.error("database is null");
		if (project == null)
			return Res.error("project is null");
		return Res.ok(project);
	}

	/// The supported files in the order in which they are imported: first the
	/// CityGML files (including those extracted from ZIP archives), then the
	/// Excel files, so that Excel rows can update the buildings that were created
	/// from the CityGML data. Unsupported files are skipped. This is
	/// package-private for testing.
	static List<File> orderedFiles(List<File> files) {
		if (files == null) return List.of();
		var gml = new ArrayList<File>();
		var excel = new ArrayList<File>();
		for (var file : files) {
			var type = ImportFileType.of(file);
			if (type == ImportFileType.CITY_GML) {
				gml.add(file);
			} else if (type == ImportFileType.EXCEL) {
				excel.add(file);
			}
		}
		gml.addAll(excel);
		return gml;
	}

	/// Imports the CityGML files first and then the Excel files; all other files
	/// are skipped. When no file has a supported type, an error is returned. This
	/// is package-private for testing.
	Res<Project> importFiles() {
		if (files == null || files.isEmpty())
			return Res.error("No import files provided");

		var gml = new ArrayList<File>();
		var excel = new ArrayList<File>();
		for (var file : orderedFiles(files)) {
			var type = ImportFileType.of(file);
			if (type == ImportFileType.CITY_GML) {
				gml.add(file);
			} else if (type == ImportFileType.EXCEL) {
				excel.add(file);
			}
		}
		if (gml.isEmpty() && excel.isEmpty())
			return Res.error("No import files provided");

		if (!gml.isEmpty()) {
			var res = importCityGmlFiles(gml);
			if (res.isError()) return res;
		}
		for (var file : excel) {
			var res = importExcelFile(file);
			if (res.isError()) return res;
		}
		return Res.ok(project);
	}

	private Res<Project> importCityGmlFiles(List<File> files) {
		if (files == null || files.isEmpty()) {
			return Res.error("No CityGML file provided");
		}
		try {
			return new CityGmlImport(db, project, files).call();
		} catch (Exception e) {
			return Res.error("project creation failed during CityGML import", e);
		}
	}

	private Res<Project> importExcelFile(File file) {
		if (file == null) {
			return Res.error("No Excel file provided");
		}
		try {
			return new XlsBuildingImport(db, project, file).call();
		} catch (Exception e) {
			return Res.error("project creation failed during Excel import", e);
		}
	}

	private Res<Project> importOsm() {
		if (project == null || project.map() == null) {
			return Res.error("project map is not initialized");
		}
		var osm = OsmStreetFetch.into(project.map());
		return osm.isError() ? osm.castError() : Res.ok(project);
	}

	/// When CityGML data were provided, the climate region was already determined
	/// in the CityGML import (before the heat demand prediction). Otherwise, we
	/// determine the climate region here.
	private Res<Project> determineClimateRegion() {
		if (project == null)
			return Res.error("project is null");
		if (project.climateRegion() != null)
			return Res.ok(project);

		var lookup = ClimateRegionLookup.lookup(db, project.map());
		if (lookup.isError()) {
			return lookup.wrapError("failed to determine climate region");
		}
		project.climateRegion(lookup.value());
		return Res.ok(project);
	}

	private Res<Project> saveProject() {
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
