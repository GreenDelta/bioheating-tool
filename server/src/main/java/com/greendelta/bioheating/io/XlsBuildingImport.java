package com.greendelta.bioheating.io;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.BuildingDefaults;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.GeoMap;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.model.RoofType;
import com.greendelta.bioheating.model.WarmWater;
import com.greendelta.bioheating.predict.BoostPredictor;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.openlca.commons.Res;
import org.openlca.commons.Strings;

public class XlsBuildingImport implements Callable<Res<Project>> {

	/// The area of the square that is used to match an Excel row to an existing
	/// building when neither the ground area nor the building type is provided
	/// (a 12 x 12 m square).
	private static final double DEFAULT_GROUND_AREA = 144;

	private static final GeometryFactory geometries = new GeometryFactory();

	private final Database db;
	private final Project project;
	private final File file;

	public XlsBuildingImport(Database db, Project project, File file) {
		this.db = db;
		this.project = project;
		this.file = file;
	}

	public Res<Project> call() {
		if (project == null) return Res.error("No project provided");

		// read the rows as patches
		var patchesRes = readPatches();
		if (patchesRes.isError()) return patchesRes.castError();
		var patches = patchesRes.value();

		// find or initialize the map
		var mapRes = initMap(patches.getFirst());
		if (mapRes.isError()) {
			return mapRes.castError();
		}
		var map = mapRes.value();

		// create the coordinate transformer
		var projRes = CoordinateTransformer.fromWgs84To(map.crs());
		if (projRes.isError()) {
			return projRes.wrapError(
				"Failed to create coordinate transformer for WGS84 -> " + map.crs());
		}
		var proj = projRes.value();

		// pass 1: project the coordinates and find the buildings to update
		var index = BuildingIndex.of(map.buildings());
		var entries = new ArrayList<Entry>();
		for (var patch : patches) {
			var entry = match(patch, proj, index);
			if (entry != null) entries.add(entry);
		}
		if (entries.isEmpty()) {
			return Res.error("No valid building data found in file");
		}

		// pass 2: resolve the attributes with the neighbor count and apply them
		for (var entry : entries) {
			apply(entry, entries, index, map);
		}

		// pass 3: estimate the heat demand and the peak load where they were
		// not provided
		var estimateRes = estimateDemand(project, map);
		if (estimateRes.isError()) return estimateRes.castError();

		return Res.ok(project);
	}

	/// Estimates the heat demand and the peak load of the buildings that do not
	/// have these values. The climate region is determined from the map when it
	/// is not set yet.
	private Res<Void> estimateDemand(Project project, GeoMap map) {
		var missing = new ArrayList<Building>();
		for (var building : map.buildings()) {
			if (building.heatDemand() <= 0 || building.peakLoad() <= 0) {
				missing.add(building);
			}
		}
		if (missing.isEmpty()) return Res.ok();

		var region = project.climateRegion();
		if (region == null) {
			var lookup = ClimateRegionLookup.lookup(db, map);
			if (lookup.isError()) {
				return lookup.wrapError(
					"Failed to determine the climate region needed for the "
						+ "heat demand estimation");
			}
			region = lookup.value();
			project.climateRegion(region);
		}

		var predictor = BoostPredictor.getDefault();
		if (predictor.isError()) {
			return predictor.wrapError("Failed to load the heat demand predictor");
		}
		var predictions = predictor.value().predictAll(region, missing);
		if (predictions.isError()) {
			return predictions.wrapError(
				"Failed to predict the heat demand and the peak load");
		}

		var values = predictions.value();
		for (int i = 0; i < missing.size(); i++) {
			var building = missing.get(i);
			var prediction = values.get(i);
			if (building.heatDemand() <= 0) {
				building.heatDemand(prediction.heatDemand());
			}
			if (building.peakLoad() <= 0) {
				building.peakLoad(prediction.peakLoad());
			}
		}
		return Res.ok();
	}

	private Res<List<XlsBuildingPatch>> readPatches() {
		if (file == null) {
			return Res.error("No valid Excel file provided");
		}
		try (var stream = new FileInputStream(file);
				 var wb = WorkbookFactory.create(stream)) {
			if (wb.getNumberOfSheets() == 0) {
				return Res.error("Excel file contains no sheets");
			}
			var sheet = wb.getSheetAt(0);
			var patches = new ArrayList<XlsBuildingPatch>();
			for (int i = 1; i <= sheet.getLastRowNum(); i++) {
				var patch = XlsBuildingPatch.of(sheet.getRow(i));
				if (patch != null && patch.isValid()) {
					patches.add(patch);
				}
			}
			return patches.isEmpty()
				? Res.error("No valid rows found in sheet")
				: Res.ok(patches);
		} catch (Exception e) {
			return Res.error("Failed to read Excel file", e);
		}
	}

	private Res<GeoMap> initMap(XlsBuildingPatch first) {
		var map = project.map();
		if (map != null && Strings.isNotBlank(map.crs())) {
			return Res.ok(map);
		}
		var crs = CrsId.utmFromWGS84(first.longitude(), first.latitude());
		if (crs.isError()) {
			return crs.wrapError("Failed to determine UTM CRS from first building");
		}
		if (map == null) {
			map = new GeoMap();
			project.map(map);
		}
		map.crs(crs.value().value());
		return Res.ok(map);
	}

	/// Projects the coordinates of the patch and finds the building that should
	/// be updated: by ID first, otherwise by the intersection of the square.
	private Entry match(
		XlsBuildingPatch patch,
		CoordinateTransformer proj,
		BuildingIndex index
	) {
		var projected = proj.project(patch.longitude(), patch.latitude());
		if (projected.isError()) return null;
		var center = new Coordinate(projected.value().x, projected.value().y);
		var matched = index.findByCityId(patch.cityId());
		var square = squareAround(center, squareAreaOf(patch, matched));
		var building = matched != null
			? matched
			: index.findIntersecting(square);
		return new Entry(patch, square, building);
	}

	/// The area of the square that is used for matching. It is derived from the
	/// provided ground area, the ground area or default area of a matched
	/// building, the default area of the provided type, or a generic default.
	private static double squareAreaOf(XlsBuildingPatch patch, Building matched) {
		if (patch.groundArea() != null) return patch.groundArea();
		if (matched != null && matched.groundArea() > 0) {
			return matched.groundArea();
		}
		if (patch.type() != null) return patch.type().defaultGroundArea();
		if (matched != null && matched.type() != null) {
			return matched.type().defaultGroundArea();
		}
		return DEFAULT_GROUND_AREA;
	}

	/// Counts the heated neighbors of the entry: the existing buildings from the
	/// index and the other rows of the file (which are all heated).
	private static int countNeighbors(
		Entry entry,
		List<Entry> entries,
		BuildingIndex index
	) {
		int count = entry.building() != null
			? index.countHeatedNeighbors(entry.building())
			: index.countHeatedNeighbors(entry.square());
		for (var other : entries) {
			if (other == entry) continue;
			if (isWithinDistance(entry.square(), other.square())) {
				count++;
			}
		}
		return count;
	}

	private static boolean isWithinDistance(Polygon a, Polygon b) {
		try {
			return a.isWithinDistance(b, BuildingIndex.NEIGHBOR_THRESHOLD);
		} catch (Exception e) {
			return false;
		}
	}

	/// Applies the patch to the building. Provided values overwrite the existing
	/// values; missing values keep the existing value when it is present and fall
	/// back to a smart default otherwise.
	private void apply(
		Entry entry,
		List<Entry> entries,
		BuildingIndex index,
		GeoMap map
	) {
		var patch = entry.patch();
		var building = entry.building();
		boolean isNew = building == null;
		if (isNew) building = new Building();

		// ID: provided, or generated for a new building
		if (patch.cityId() != null) {
			building.cityId(patch.cityId());
		}
		if (Strings.isBlank(building.cityId())) {
			building.cityId(UUID.randomUUID().toString());
		}

		// name: provided, keep the existing one, or derive it
		if (patch.name() != null) {
			building.name(patch.name());
		} else if (Strings.isBlank(building.name())) {
			building.name(patch.nameOrDefault());
		}

		// address
		if (patch.locality() != null) building.locality(patch.locality());
		if (patch.postalCode() != null) building.postalCode(patch.postalCode());
		if (patch.street() != null) building.street(patch.street());
		if (patch.streetNumber() != null) {
			building.streetNumber(patch.streetNumber());
		}

		// type, height and ground area; the neighbor count is only computed when
		// the type has to be estimated from it
		var defaults = BuildingDefaults.resolve(
			patch.type() != null ? patch.type() : building.type(),
			patch.height() != null ? patch.height() : building.height(),
			patch.groundArea() != null
				? patch.groundArea()
				: building.groundArea(),
			() -> countNeighbors(entry, entries, index)
		);
		building.type(defaults.type())
			.height(defaults.height())
			.groundArea(defaults.groundArea());

		// construction age and roof type
		if (patch.constructionAge() != null) {
			building.constructionAge(patch.constructionAge());
		}
		if (patch.flatRoof() != null) {
			building.roofType(
				patch.flatRoof() ? RoofType.FLAT : RoofType.PITCHED);
		}

		// warm water fraction
		if (patch.warmWaterFraction() != null) {
			building.warmWaterFraction(patch.warmWaterFraction());
		} else if (!(building.warmWaterFraction() > 0)) {
			building.warmWaterFraction(
				WarmWater.of(building.type(), building.constructionAge()));
		}

		// heat demand and peak load: provided values overwrite, missing values
		// keep the existing value (estimation is done in a later step)
		if (patch.heatDemand() != null) {
			building.heatDemand(patch.heatDemand());
		}
		if (patch.peakLoad() != null) {
			building.peakLoad(patch.peakLoad());
		}

		// geometry: replace the polygon with the square when it is larger
		var current = BuildingIndex.polygonOf(building);
		if (current == null || entry.square().getArea() > current.getArea()) {
			building.coordinates(entry.square().getCoordinates());
		}

		// all buildings from an Excel file are heated and included (supply
		// centers stay excluded per the model invariants)
		if (building.isSupplyCenter()) {
			building.isHeated(false).isIncluded(false);
		} else {
			building.isHeated(true).isIncluded(true);
		}

		if (isNew) {
			map.buildings().add(building);
		}
	}


	/// The square that is used as the building polygon and for matching. Its
	/// area is the given area, so its side length is `sqrt(area)`.
	private static Polygon squareAround(Coordinate center, double area) {
		double d = Math.sqrt(area) / 2;
		return geometries.createPolygon(new Coordinate[] {
			new Coordinate(center.x - d, center.y - d),
			new Coordinate(center.x + d, center.y - d),
			new Coordinate(center.x + d, center.y + d),
			new Coordinate(center.x - d, center.y + d),
			new Coordinate(center.x - d, center.y - d),
		});
	}

	private record Entry(
		XlsBuildingPatch patch,
		Polygon square,
		Building building
	) {}
}
