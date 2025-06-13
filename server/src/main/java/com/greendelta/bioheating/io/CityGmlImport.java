package com.greendelta.bioheating.io;

import java.io.File;
import java.util.concurrent.Callable;

import org.locationtech.jts.geom.Coordinate;

import com.greendelta.bioheating.citygml.GmlBuilding;
import com.greendelta.bioheating.citygml.GmlModel;
import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.GeoMap;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.util.Res;
import com.greendelta.bioheating.util.Strings;

public class CityGmlImport implements Callable<Res<Project>> {

	private final Database db;
	private final Project project;
	private final File file;

	public CityGmlImport(
		Database db, Project project, File file
	) {
		this.db = db;
		this.project = project;
		this.file = file;
	}

	@Override
	public Res<Project> call() {
		if (project == null)
			return Res.error("project cannot be null");
		if (file == null || !file.exists())
			return Res.error("file does not exist");
		var res = GmlModel.readFrom(file);
		if (res.hasError())
			return res.castError();

		var model = res.value();
		var mapRes = initMap(model);
		if (mapRes.hasError())
			return mapRes.castError();
		var map = mapRes.value();

		for (var b : model.buildings()) {
			var building = convertBuilding(b);
			if (building != null) {
				map.buildings().add(building);
			}
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

		var map = project.map();
		if (map != null) {
			return Strings.eq(env.srs(), map.crs())
				? Res.of(map)
				: Res.error("different CSR of model and current project map: "
				+ map.crs() + " vs. " + env.srs());
		}

		map = new GeoMap().crs(env.srs());
		project.map(map);
		return Res.of(map);
	}
	private Building convertBuilding(GmlBuilding b) {
		if (b == null)
			return null;
		var cs = coordinatesOf(b);
		if (cs == null)
			return null;

		// Calculate ground area from coordinates
		double groundArea = calculateGroundArea(cs);

		// For now, assume heated area is 80% of ground area (this could be enhanced)
		double heatedArea = groundArea * 0.8;

		// Calculate volume using height and ground area
		double volume = b.height() > 0 ? groundArea * b.height() : 0;

		return new Building()
			.name(nameOf(b))
			.coordinates(cs)
			.roofType(b.roofType())
			.function(b.function())
			.height(b.height())
			.storeys(b.storeys())
			.groundArea(groundArea)
			.heatedArea(heatedArea)
			.volume(volume);
	}

	private String nameOf(GmlBuilding b) {
		var address = b.address();
		if (address == null)
			return b.id();

		var street = address.street();
		var number = address.number();
		if (Strings.isNil(street))
			return b.id();
		return Strings.isNil(number)
			? street
			: street + " " + number;
	}

	private Coordinate[] coordinatesOf(GmlBuilding b) {
		var polygon = b.groundSurface();
		if (polygon == null)
			return null;
		var shell = polygon.getExteriorRing();
		return shell != null
			? shell.getCoordinates()
			: null;
	}

	private double calculateGroundArea(Coordinate[] coordinates) {
		if (coordinates == null || coordinates.length < 3)
			return 0.0;

		// Using the shoelace formula to calculate polygon area
		double area = 0.0;
		int n = coordinates.length - 1; // Exclude the last coordinate if it's the same as first

		for (int i = 0; i < n; i++) {
			int j = (i + 1) % n;
			area += coordinates[i].x * coordinates[j].y;
			area -= coordinates[j].x * coordinates[i].y;
		}

		return Math.abs(area) / 2.0;
	}
}
