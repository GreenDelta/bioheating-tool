package com.greendelta.bioheating.io;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

public class CrsIdTest {

	@Test
	public void testSelf() {
		var crss = List.of(CrsId.wgs84(), CrsId.utm32(), CrsId.utm33());
		for (var crs : crss) {
			assertEquals(crs.value(), "EPSG:" + crs.code());
			assertEquals(crs, CrsId.parse(crs.value()));
		}
	}

	@Test
	public void testParse() {
		assertEquals(CrsId.utm33(), CrsId.parse("EPSG:25833"));
		assertEquals(
			CrsId.utm32(),
			CrsId.parse("urn:adv:crs:ETRS89_UTM32*DE_DHHN2016_NH")
		);
		assertEquals(
			CrsId.utm33(),
			CrsId.parse("urn:adv:crs:ETRS89_UTM33*DE_DHHN2016_NH")
		);
		assertEquals(
			CrsId.utm33(),
			CrsId.parse("urn:ogc:def:crs,crs:EPSG:6.12:25833,crs:EPSG:6.12:5783")
		);
		assertEquals(CrsId.utm32(), CrsId.parse("etrs89_utm32"));
		assertEquals(CrsId.utm33(), CrsId.parse("etrs89_utm33"));
		assertEquals(CrsId.utm32(), CrsId.parse("ETRS89_UTM32"));
		assertEquals("unknown:123", CrsId.parse("unknown:123").value());
	}

	@Test
	public void testUtmFromWGS84() {
		double lon = 13.3777;
		double lat = 52.5162;
		var utm = CrsId.utmFromWGS84(lon, lat).orElseThrow();
		var wgs84 = CrsId.wgs84();

		var projected = CoordinateTransformer
			.of(wgs84, utm)
			.orElseThrow()
			.project(lon, lat)
			.orElseThrow();

		var origin = CoordinateTransformer
			.of(utm, wgs84)
			.orElseThrow()
			.project(projected.x, projected.y)
			.orElseThrow();

		assertEquals(lon, origin.x, 1e-4);
		assertEquals(lat, origin.y, 1e-4);
	}
}
