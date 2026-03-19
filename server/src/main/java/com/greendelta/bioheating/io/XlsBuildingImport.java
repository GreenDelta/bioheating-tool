package com.greendelta.bioheating.io;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.BuildingType;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.GeoMap;
import com.greendelta.bioheating.model.Project;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.openlca.commons.Res;
import org.openlca.commons.Strings;

public class XlsBuildingImport implements Callable<Res<Project>> {

	private final Database db;
	private final Project project;
	private final File file;
	private final DataFormatter cells = new DataFormatter();
	private final GeometryFactory geometries = new GeometryFactory();

	public XlsBuildingImport(Database db, Project project, File file) {
		this.db = db;
		this.project = project;
		this.file = file;
	}

	public Res<Project> call() {
		if (db == null) return Res.error("database is null");
		if (project == null) return Res.error("project is null");
		if (file == null) return Res.error("Excel file is null");

		var rows = readRows();
		if (rows.isError()) return rows.castError();
		var items = rows.value();

		var first = firstValid(items);
		if (first == null) {
			return Res.error("No valid buildings found in Excel file");
		}

		var map = initMap(first);
		if (map.isError()) return map.castError();

		var transform = CoordinateTransformer.fromWgs84To(map.value().crs());
		if (transform.isError()) return transform.wrapError(
			"failed to create coordinate transformer for project map"
		);

		var byCityId = new HashMap<String, Building>();
		for (var building : map.value().buildings()) {
			if (building != null && Strings.isNotBlank(building.cityId())) {
				byCityId.put(building.cityId(), building);
			}
		}

		int imported = 0;
		for (var item : items) {
			if (!item.isValid()) continue;

			var center = transform.value().project(item.longitude(), item.latitude());
			if (center.isError()) continue;

			var building = Strings.isNotBlank(item.cityId())
				? byCityId.get(item.cityId())
				: null;
			boolean isNew = building == null;
			if (isNew) {
				building = new Building();
			}

			update(building, item, new Coordinate(center.value().x, center.value().y));
			if (isNew) {
				map.value().buildings().add(building);
				if (Strings.isNotBlank(building.cityId())) {
					byCityId.put(building.cityId(), building);
				}
			}
			imported++;
		}

		if (imported == 0) {
			return Res.error("No valid buildings could be imported");
		}

		var next = project.id() == 0 ? db.insert(project) : db.update(project);
		return Res.ok(next);
	}

	private Res<List<RowData>> readRows() {
		try (
			var stream = new FileInputStream(file);
			var workbook = WorkbookFactory.create(stream)
		) {
			if (workbook.getNumberOfSheets() == 0) {
				return Res.error("Excel file contains no sheets");
			}
			Sheet sheet = workbook.getSheetAt(0);
			var rows = new ArrayList<RowData>();
			for (int i = 1; i <= sheet.getLastRowNum(); i++) {
				rows.add(readRow(sheet.getRow(i)));
			}
			return Res.ok(rows);
		} catch (Exception e) {
			return Res.error("Failed to read Excel file", e);
		}
	}

	private RowData readRow(Row row) {
		if (row == null) return RowData.empty();
		return new RowData(
			textOf(row, 0),
			textOf(row, 1),
			doubleOf(row, 2),
			doubleOf(row, 3),
			doubleOf(row, 4),
			doubleOf(row, 5),
			boolOf(row, 6),
			intOf(row, 7),
			textOf(row, 8),
			textOf(row, 9),
			textOf(row, 10),
			textOf(row, 11)
		);
	}

	private RowData firstValid(List<RowData> items) {
		for (var item : items) {
			if (item.isValid()) {
				return item;
			}
		}
		return null;
	}

	private Res<GeoMap> initMap(RowData first) {
		var map = project.map();
		if (map != null && Strings.isNotBlank(map.crs())) {
			return Res.ok(map);
		}

		var crs = CrsId.utmFromWGS84(first.longitude(), first.latitude());
		if (crs.isError()) return crs.wrapError(
			"failed to determine UTM CRS from first building"
		);

		if (map == null) {
			map = new GeoMap();
			project.map(map);
		}
		map.crs(crs.value().value());
		return Res.ok(map);
	}

	private void update(Building building, RowData row, Coordinate center) {
		building.cityId(blankToNull(row.cityId()));
		building.name(row.name().strip());
		building.type(
			row.buildingTypeCode() != null
				? BuildingType.of(row.buildingTypeCode())
				: BuildingType.OTHER
		);
		building.locality(blankToNull(row.locality()));
		building.postalCode(blankToNull(row.postalCode()));
		building.street(blankToNull(row.street()));
		building.streetNumber(blankToNull(row.streetNumber()));

		if (!keepPolygon(building, center)) {
			building.coordinates(squareAround(center));
			building.groundArea(12 * 12);
		}

		var heated = row.heatDemand() != null && row.peakLoad() != null;
		building.isHeated(heated);
		building.heatDemand(row.heatDemand() != null ? row.heatDemand() : 0);
		building.peakLoad(row.peakLoad() != null ? row.peakLoad() : 0);
		building.isIncluded(
			heated && (row.isIncluded() != null ? row.isIncluded() : true)
		);
	}

	private boolean keepPolygon(Building building, Coordinate center) {
		if (building == null || center == null || building.coordinates() == null) {
			return false;
		}
		var coords = normalized(building.coordinates());
		if (coords == null || coords.length < 4) {
			return false;
		}
		var polygon = geometries.createPolygon(coords);
		return polygon.covers(geometries.createPoint(center));
	}

	private Coordinate[] squareAround(Coordinate center) {
		double d = 6.0;
		return new Coordinate[] {
			new Coordinate(center.x - d, center.y - d),
			new Coordinate(center.x + d, center.y - d),
			new Coordinate(center.x + d, center.y + d),
			new Coordinate(center.x - d, center.y + d),
			new Coordinate(center.x - d, center.y - d),
		};
	}

	private Coordinate[] normalized(Coordinate[] coords) {
		if (coords == null || coords.length == 0) return null;
		var first = coords[0];
		var last = coords[coords.length - 1];
		if (first != null && last != null && first.equals2D(last)) {
			return coords;
		}
		var ring = new Coordinate[coords.length + 1];
		System.arraycopy(coords, 0, ring, 0, coords.length);
		ring[ring.length - 1] = new Coordinate(first.x, first.y, first.z);
		return ring;
	}

	private String textOf(Row row, int column) {
		var cell = row.getCell(column);
		if (cell == null) return null;
		var value = cells.formatCellValue(cell);
		return Strings.isBlank(value) ? null : value.strip();
	}

	private Double doubleOf(Row row, int column) {
		var text = textOf(row, column);
		if (Strings.isBlank(text)) return null;
		try {
			return Double.parseDouble(text.replace(',', '.'));
		} catch (Exception e) {
			return null;
		}
	}

	private Integer intOf(Row row, int column) {
		var value = doubleOf(row, column);
		return value != null ? (int) Math.round(value) : null;
	}

	private Boolean boolOf(Row row, int column) {
		var text = textOf(row, column);
		if (Strings.isBlank(text)) return null;
		return switch (text.strip().toLowerCase()) {
			case "1", "true" -> true;
			case "0", "false" -> false;
			default -> null;
		};
	}

	private String blankToNull(String value) {
		return Strings.isBlank(value) ? null : value.strip();
	}

	private record RowData(
		String cityId,
		String name,
		Double longitude,
		Double latitude,
		Double heatDemand,
		Double peakLoad,
		Boolean isIncluded,
		Integer buildingTypeCode,
		String locality,
		String postalCode,
		String street,
		String streetNumber
	) {
		static RowData empty() {
			return new RowData(null, null, null, null, null, null, null, null, null, null, null, null);
		}

		boolean isValid() {
			return Strings.isNotBlank(name)
				&& longitude != null
				&& latitude != null
				&& longitude >= -180
				&& longitude <= 180
				&& latitude >= -90
				&& latitude <= 90;
		}
	}
}
