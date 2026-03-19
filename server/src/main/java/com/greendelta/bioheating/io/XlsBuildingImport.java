package com.greendelta.bioheating.io;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.BuildingType;
import com.greendelta.bioheating.model.GeoMap;
import com.greendelta.bioheating.model.Project;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.locationtech.jts.geom.Coordinate;
import org.openlca.commons.Res;
import org.openlca.commons.Strings;

public class XlsBuildingImport implements Callable<Res<Project>> {

	private final Project project;
	private final File file;

	public XlsBuildingImport(Project project, File file) {
		this.project = project;
		this.file = file;
	}

	public Res<Project> call() {
		if (project == null) return Res.error("No project provided");

		// read the rows
		var rowsRes = readRows();
		if (rowsRes.isError()) {
			return rowsRes.wrapError("Failed to read fows from Excel file");
		}
		var rows = rowsRes.value();

		// find or initialize the map
		var mapRes = initMap(rows.getFirst());
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

		// index the existing buildings
		var existing = new HashMap<String, Building>();
		for (var b : map.buildings()) {
			if (Strings.isNotBlank(b.cityId())) {
				existing.put(b.cityId(), b);
			}
		}

		// create or update the buildings
		for (var r : rows) {

			// project the coordinate to UTM
			var coo = proj.project(r.longitude(), r.latitude());
			if (coo.isError()) continue;
			var xy = coo.value();

			// find or initialize the building
			var b = Strings.isNotBlank(r.cityId())
				? existing.get(r.cityId())
				: null;
			boolean isNew = b == null;
			if (isNew) {
				b = new Building();
				var center = new Coordinate(xy.x, xy.y);
				b.coordinates(squareAround(center));
			}

			update(b, r);
			if (isNew) {
				map.buildings().add(b);
				if (Strings.isNotBlank(b.cityId())) {
					existing.put(b.cityId(), b);
				}
			}
		}

		return map.buildings().isEmpty()
			? Res.error("No valid building data found in file")
			: Res.ok(project);
	}

	private Res<List<RowData>> readRows() {
		if (file == null) {
			return Res.error("No valid Excel file provided");
		}
		try (var stream = new FileInputStream(file);
				 var wb = WorkbookFactory.create(stream)) {
			if (wb.getNumberOfSheets() == 0) {
				return Res.error("Excel file contains no sheets");
			}
			var sheet = wb.getSheetAt(0);
			var rows = new ArrayList<RowData>();
			for (int i = 1; i <= sheet.getLastRowNum(); i++) {
				var row = sheet.getRow(i);
				if (row == null) continue;
				var rowData = RowData.of(row);
				if (rowData.isValid()) {
					rows.add(rowData);
				}
			}
			return rows.isEmpty()
				? Res.error("No valid rows found in sheet")
				: Res.ok(rows);
		} catch (Exception e) {
			return Res.error("Failed to read Excel file", e);
		}
	}

	private Res<GeoMap> initMap(RowData first) {
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

	private void update(Building b, RowData row) {
		b.cityId(row.cityId())
			.name(row.name().strip())
			.type(BuildingType.of(row.buildingTypeCode()))
			.locality(row.locality())
			.postalCode(row.postalCode())
			.street(row.street())
			.streetNumber(row.streetNumber());

		var heated = row.heatDemand() > 0 && row.peakLoad() > 0;
		b.isHeated(heated)
			.heatDemand(row.heatDemand())
			.peakLoad(row.peakLoad())
			.isIncluded(row.isIncluded());
	}


	private Coordinate[] squareAround(Coordinate center) {
		double d = 6.0;
		return new Coordinate[]{
			new Coordinate(center.x - d, center.y - d),
			new Coordinate(center.x + d, center.y - d),
			new Coordinate(center.x + d, center.y + d),
			new Coordinate(center.x - d, center.y + d),
			new Coordinate(center.x - d, center.y - d),
		};
	}

	private record RowData(
		String cityId,
		String name,
		double longitude,
		double latitude,
		double heatDemand,
		double peakLoad,
		boolean isIncluded,
		int buildingTypeCode,
		String locality,
		String postalCode,
		String street,
		String streetNumber
	) {

		static RowData of(Row row) {
			if (row == null) return null;
			return new RowData(
				textOf(row, 0),
				textOf(row, 1),
				numOf(row, 2),
				numOf(row, 3),
				numOf(row, 4),
				numOf(row, 5),
				boolOf(row, 6),
				(int) numOf(row, 7),
				textOf(row, 8),
				textOf(row, 9),
				textOf(row, 10),
				textOf(row, 11)
			);
		}

		private static String textOf(Row row, int col) {
			var cell = row.getCell(col);
			if (cell == null) return null;
			var s = cell.getCellType() == CellType.STRING
				? cell.getStringCellValue()
				: null;
			return s != null ? s.trim() : null;
		}

		private static double numOf(Row row, int col) {
			var cell = row.getCell(col);
			if (cell == null) return 0;
			return cell.getCellType() == CellType.NUMERIC
				? cell.getNumericCellValue()
				: 0;
		}

		private static boolean boolOf(Row row, int col) {
			var cell = row.getCell(col);
			if (cell == null) return false;
			return switch (cell.getCellType()) {
				case BOOLEAN -> cell.getBooleanCellValue();
				case NUMERIC -> {
					var num = cell.getNumericCellValue();
					yield num != 0;
				}
				case STRING -> {
					var s = cell.getStringCellValue();
					if (Strings.isBlank(s)) yield false;
					yield switch (s.trim().toLowerCase(Locale.ROOT)) {
						case "t", "true", "y", "yes", "j", "ja", "1" -> true;
						default -> false;
					};
				}
				default -> false;
			};
		}

		boolean isValid() {
			return Strings.isNotBlank(name)
				&& longitude != 0
				&& latitude != 0
				&& longitude >= -180
				&& longitude <= 180
				&& latitude >= -90
				&& latitude <= 90;
		}
	}
}
