package com.greendelta.bioheating.io;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.BuildingType;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.model.RoofType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.openlca.commons.Res;
import org.openlca.commons.Strings;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.Set;

public class XlsBuildingExport {

	private final Project project;
	private final OutputStream stream;
	private Set<Long> ids;
	private final GeometryFactory geometries = new GeometryFactory();

	public XlsBuildingExport(Project project, OutputStream stream) {
		this.project = project;
		this.stream = stream;
	}

	/// Sets the IDs of the buildings that should be exported. If this
	/// is not set, all buildings of the project will be exported.
	public XlsBuildingExport withIds(Set<Long> ids) {
		this.ids = ids;
		return this;
	}

	public Res<Void> run() {
		if (project == null) return Res.error("project is null");
		if (project.map() == null) return Res.error("project has no map");

		var wgs84 = CoordinateTransformer.toWgs84From(project.map());
		if (wgs84.isError()) return wgs84.wrapError(
			"failed to create WGS84 coordinate transformer"
		);

		try (Workbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("Buildings");
			int rowIndex = 0;
			writeHeader(sheet.createRow(rowIndex++));
			for (var b : project.map().buildings()) {
				if (shouldSkip(b)) continue;
				var row = sheet.createRow(rowIndex++);
				append(row, b, wgs84.value());
			}
			for (int i = 0; i < 16; i++) {
				sheet.autoSizeColumn(i);
			}
			workbook.write(stream);
			return Res.ok();
		} catch (Exception e) {
			return Res.error("Failed to export building data", e);
		}
	}

	private void writeHeader(Row row) {
		putString(row, 0, "id");
		putString(row, 1, "name");
		putString(row, 2, "longitude");
		putString(row, 3, "latitude");
		putString(row, 4, "building type");
		putString(row, 5, "construction year");
		putString(row, 6, "height");
		putString(row, 7, "ground area");
		putString(row, 8, "flat roof");
		putString(row, 9, "warm water fraction");
		putString(row, 10, "heat demand");
		putString(row, 11, "peak load");
		putString(row, 12, "city");
		putString(row, 13, "postal code");
		putString(row, 14, "street");
		putString(row, 15, "number");
	}

	private void append(Row row, Building building, CoordinateTransformer wgs84) {
		var center = centerOf(building);
		Coordinate point = null;
		if (center != null) {
			var projected = wgs84.project(center.x, center.y);
			if (projected.isOk()) {
				point = new Coordinate(projected.value().x, projected.value().y);
			}
		}

		putString(row, 0, building.cityId());
		putString(row, 1, nameOf(building));
		putNumber(row, 2, point != null ? point.x : null);
		putNumber(row, 3, point != null ? point.y : null);
		putNumber(row, 4, typeCodeOf(building.type()));
		putString(row, 5, constructionYearOf(building));
		putNumber(row, 6, positive(building.height()));
		putNumber(row, 7, positive(building.groundArea()));
		putBoolean(row, 8, isFlatRoof(building));
		putNumber(row, 9, positive(building.warmWaterFraction()));
		putNumber(row, 10, positive(building.heatDemand()));
		putNumber(row, 11, positive(building.peakLoad()));
		putString(row, 12, building.locality());
		putString(row, 13, building.postalCode());
		putString(row, 14, building.street());
		putString(row, 15, building.streetNumber());
	}

	private Coordinate centerOf(Building building) {
		if (building == null || building.coordinates() == null) return null;
		var coords = normalized(building.coordinates());
		if (coords == null || coords.length < 4) return null;
		return geometries.createPolygon(coords).getCentroid().getCoordinate();
	}

	private Coordinate[] normalized(Coordinate[] coords) {
		if (coords == null || coords.length == 0) return null;
		var copy = Arrays.copyOf(coords, coords.length);
		var first = copy[0];
		var last = copy[copy.length - 1];
		if (first != null && last != null && first.equals2D(last)) {
			return copy;
		}
		copy = Arrays.copyOf(copy, copy.length + 1);
		copy[copy.length - 1] = new Coordinate(first.x, first.y, first.z);
		return copy;
	}

	private String nameOf(Building building) {
		if (building == null) return null;
		if (Strings.isNotBlank(building.name())) return building.name();
		if (Strings.isBlank(building.street())) return null;
		return Strings.isBlank(building.streetNumber())
			? building.street()
			: building.street() + " " + building.streetNumber();
	}

	/// The building type code, or `null` when the type is not set.
	private static Double typeCodeOf(BuildingType type) {
		return type != null ? (double) type.code() : null;
	}

	/// The construction year as a range string (the label of the age), or
	/// `null` when it is not set.
	private static String constructionYearOf(Building building) {
		var age = building.constructionAge();
		return age != null ? age.toString() : null;
	}

	/// The model only distinguishes flat and pitched roofs; a missing roof type
	/// is written as pitched (the default).
	private static boolean isFlatRoof(Building building) {
		return building.roofType() == RoofType.FLAT;
	}

	/// `<= 0` means _not set_; such values are written as empty cells.
	private static Double positive(double value) {
		return value > 0 ? value : null;
	}

	private void putString(Row row, int column, String value) {
		Cell cell = row.createCell(column);
		if (value != null) {
			cell.setCellValue(value);
		}
	}

	private void putNumber(Row row, int column, Double value) {
		Cell cell = row.createCell(column);
		if (value != null) {
			cell.setCellValue(value);
		}
	}

	private void putNumber(Row row, int column, double value) {
		row.createCell(column).setCellValue(value);
	}

	private void putBoolean(Row row, int column, boolean value) {
		row.createCell(column).setCellValue(value);
	}

	private void putNumber(Row row, int column, int value) {
		row.createCell(column).setCellValue(value);
	}

	private boolean shouldSkip(Building b) {
		if (b == null) return true;
		if (ids == null) return false;
		return !ids.contains(b.id());
	}

}

