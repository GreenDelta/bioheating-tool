package com.greendelta.bioheating.io;

import java.io.File;
import java.util.concurrent.Callable;

import org.locationtech.jts.geom.Coordinate;

import com.greendelta.bioheating.citygml.GmlAddress;
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
	private final Mappings mappings;

	public CityGmlImport(
		Database db, Project project, File file
	) {
		this.db = db;
		this.project = project;
		this.file = file;
		this.mappings = Mappings.read().orElse(null);
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

		double height = b.height();
		int storeys = storeysOf(b, height);
		double groundArea = b.groundSurface() != null
			? b.groundSurface().getArea()
			: 0;
		double totalArea = groundArea * storeys;
		double heatedArea = heatedAreaOf(totalArea, b.function());
		double volume = volumeOf(groundArea, height, b.roofType());

		var building = new Building()
			.name(nameOf(b))
			.coordinates(cs)
			.roofType(b.roofType())
			.function(b.function())
			.height(height)
			.storeys(storeys)
			.groundArea(groundArea)
			.heatedArea(heatedArea)
			.volume(volume)
			.climateZone(climateZoneOf(b));
		mapAddress(b.address(), building);
		return building;
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

	private void mapAddress(GmlAddress a, Building b) {
		if (a == null)
			return;
		b.country(a.country())
			.locality(a.locality())
			.postalCode(a.postalCode())
			.street(a.street())
			.streetNumber(a.number());
	}

	private int climateZoneOf(GmlBuilding building) {
		var key = building.attributes().get("Gemeindeschluessel");
		return key != null
			? mappings.weatherStation(key).orElse(0)
			: 0;
	}
}
