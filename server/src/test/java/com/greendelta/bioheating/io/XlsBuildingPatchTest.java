package com.greendelta.bioheating.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.greendelta.bioheating.model.BuildingType;
import com.greendelta.bioheating.model.ConstructionAge;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class XlsBuildingPatchTest {

	private Sheet sheet;
	private int rowIndex;

	@BeforeEach
	void setUp() {
		sheet = new XSSFWorkbook().createSheet();
		rowIndex = 0;
	}

	@Test
	void parsesAllColumns() {
		var patch = XlsBuildingPatch.of(row(
			"b-1", "Main Street 1", 8.68, 50.11, 6.0, "1979-1994",
			9.0, 120.0, true, 15.0, 14000.0, 6.5,
			"Berlin", "10115", "Main Street", "1"
		));

		assertEquals("b-1", patch.cityId());
		assertEquals("Main Street 1", patch.name());
		assertEquals(8.68, patch.longitude(), 1e-6);
		assertEquals(50.11, patch.latitude(), 1e-6);
		assertEquals(BuildingType.SINGLE_FAMILY, patch.type());
		assertEquals(ConstructionAge.AGE_1979_1995, patch.constructionAge());
		assertEquals(9.0, patch.height(), 1e-6);
		assertEquals(120.0, patch.groundArea(), 1e-6);
		assertEquals(Boolean.TRUE, patch.flatRoof());
		assertEquals(15.0, patch.warmWaterFraction(), 1e-6);
		assertEquals(14000.0, patch.heatDemand(), 1e-6);
		assertEquals(6.5, patch.peakLoad(), 1e-6);
		assertEquals("Berlin", patch.locality());
		assertEquals("10115", patch.postalCode());
		assertEquals("Main Street", patch.street());
		assertEquals("1", patch.streetNumber());
	}

	@Test
	void treatsMissingAndNonPositiveValuesAsNotSet() {
		var empty = XlsBuildingPatch.of(row());
		assertNull(empty.cityId());
		assertNull(empty.name());
		assertNull(empty.type());
		assertNull(empty.constructionAge());
		assertNull(empty.height());
		assertNull(empty.groundArea());
		assertNull(empty.flatRoof());
		assertNull(empty.warmWaterFraction());
		assertNull(empty.heatDemand());
		assertNull(empty.peakLoad());
		assertNull(empty.locality());

		var blank = patch(8.68, 50.11);
		blank.createCell(0).setCellValue("");
		blank.createCell(4).setCellValue(0);
		blank.createCell(6).setCellValue(-1);
		blank.createCell(7).setCellValue(0);
		var patch = XlsBuildingPatch.of(blank);
		assertNull(patch.cityId());
		assertNull(patch.type());
		assertNull(patch.height());
		assertNull(patch.groundArea());
		assertNull(patch.warmWaterFraction());
		assertNull(patch.heatDemand());
		assertNull(patch.peakLoad());
	}

	@Test
	void parsesTheFlatRoofFlag() {
		assertNull(value(8, "").flatRoof());
		assertEquals(Boolean.TRUE, value(8, true).flatRoof());
		assertEquals(Boolean.FALSE, value(8, false).flatRoof());
		assertEquals(Boolean.TRUE, value(8, "yes").flatRoof());
		assertEquals(Boolean.TRUE, value(8, "ja").flatRoof());
		assertEquals(Boolean.FALSE, value(8, "no").flatRoof());
	}

	@Test
	void parsesBuildingTypeCodes() {
		assertEquals(BuildingType.MULTI_GENERATION, value(4, 10.0).type());
		assertEquals(BuildingType.SINGLE_FAMILY, value(4, 6.0).type());
		// unknown codes are not set, so that the type can be estimated
		assertNull(value(4, 0.0).type());
		assertNull(value(4, 99.0).type());
		assertNull(value(4, "").type());
	}

	@Test
	void parsesTheConstructionYear() {
		assertEquals(
			ConstructionAge.AGE_1979_1995, value(5, 4.0).constructionAge());
		assertEquals(
			ConstructionAge.AGE_1995_2009, value(5, 2000.0).constructionAge());
		assertEquals(
			ConstructionAge.AGE_1979_1995,
			value(5, "1979-1994").constructionAge());
		assertNull(value(5, "").constructionAge());
	}

	@Test
	void derivesTheNameAndValidatesTheRow() {
		var full = XlsBuildingPatch.of(row("b-1", "Main Street 1", 8.68, 50.11));
		assertEquals("Main Street 1", full.nameOrDefault());
		assertTrue(full.isValid());

		var address = XlsBuildingPatch.of(row(
			null, null, 8.68, 50.11, null, null, null, null, null, null, null,
			null, null, null, "Main Street", "1"
		));
		assertEquals("Main Street 1", address.nameOrDefault());
		assertTrue(address.isValid());

		var byId = XlsBuildingPatch.of(row("b-1", null, 8.68, 50.11));
		assertEquals("b-1", byId.nameOrDefault());
		assertTrue(byId.isValid());

		var anonymous = XlsBuildingPatch.of(row(null, null, 8.68, 50.11));
		assertNull(anonymous.nameOrDefault());
		assertFalse(anonymous.isValid());

		var noCoordinates = XlsBuildingPatch.of(row("b-1", "name", 0, 0));
		assertFalse(noCoordinates.isValid());

		var outOfRange = XlsBuildingPatch.of(row("b-1", "name", 200, 50.11));
		assertFalse(outOfRange.isValid());
	}

	@Test
	void readsNumericValuesWithoutDecimals() {
		var patch = XlsBuildingPatch.of(row(12345.0, "name", 8.68, 50.11));
		assertEquals("12345", patch.cityId());
	}

	@Test
	void returnsNullForANullRow() {
		assertNull(XlsBuildingPatch.of(null));
	}

	private XlsBuildingPatch value(int column, Object cellValue) {
		var values = new Object[16];
		values[0] = "b-1";
		values[1] = "name";
		values[2] = 8.68;
		values[3] = 50.11;
		values[column] = cellValue;
		return XlsBuildingPatch.of(row(values));
	}

	private Row row(Object... values) {
		var row = patch(0, 0);
		for (int i = 0; i < values.length; i++) {
			var value = values[i];
			if (value == null) continue;
			var cell = row.createCell(i);
			if (value instanceof String text) {
				cell.setCellValue(text);
			} else if (value instanceof Number number) {
				cell.setCellValue(number.doubleValue());
			} else if (value instanceof Boolean flag) {
				cell.setCellValue(flag);
			} else {
				throw new IllegalArgumentException(
					"unsupported cell value: " + value);
			}
		}
		return row;
	}

	private Row patch(double longitude, double latitude) {
		var row = sheet.createRow(rowIndex++);
		row.createCell(2).setCellValue(longitude);
		row.createCell(3).setCellValue(latitude);
		return row;
	}
}
