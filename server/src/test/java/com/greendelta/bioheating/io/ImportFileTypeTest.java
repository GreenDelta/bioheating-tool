package com.greendelta.bioheating.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ImportFileTypeTest {

	@Test
	public void testExcel() {
		assertEquals(
			ImportFileType.EXCEL, ImportFileType.ofName("buildings.xlsx"));
		assertEquals(
			ImportFileType.EXCEL, ImportFileType.ofName("BUILDINGS.XLSX"));
		assertNull(ImportFileType.ofName("buildings.xls"));
		assertNull(ImportFileType.ofName("buildings.csv"));
	}

	@Test
	public void testCityGml() {
		assertEquals(
			ImportFileType.CITY_GML, ImportFileType.ofName("model.gml"));
		assertEquals(
			ImportFileType.CITY_GML, ImportFileType.ofName("nested/model.xml"));
		assertEquals(
			ImportFileType.CITY_GML, ImportFileType.ofName("model.CityGML"));
	}

	@Test
	public void testUnsupportedNames() {
		assertNull(ImportFileType.ofName(null));
		assertNull(ImportFileType.ofName(""));
		assertNull(ImportFileType.ofName("readme.txt"));
		assertNull(ImportFileType.ofName("data.zip"));
		assertNull(ImportFileType.ofName("no-extension"));
		assertNull(ImportFileType.ofName(".DS_Store"));
	}

	@Test
	public void testMacOsMetadata() {
		assertNull(ImportFileType.ofName("__MACOSX/._model.gml"));
		assertNull(ImportFileType.ofName("._model.gml"));
	}

	@Test
	public void testIsSupported() {
		assertTrue(ImportFileType.isSupported("model.citygml"));
		assertFalse(ImportFileType.isSupported("notes.txt"));
	}
}
