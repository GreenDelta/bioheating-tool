package com.greendelta.bioheating.io;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;

import com.greendelta.bioheating.model.GeoMap;
import com.greendelta.bioheating.util.Res;
import com.greendelta.bioheating.util.Strings;


public class Wsg84Transformer {

	private final CoordinateTransform fn;
	private GeometryFactory _factory;

	private Wsg84Transformer(CoordinateTransform fn) {
		this.fn = fn;
	}

	public static Res<Wsg84Transformer> getForModel(GeoMap map) {
		if (map == null)
			return Res.error("map is null");
		return Strings.isNil(map.crs())
			? Res.error("CRS of model is not defined")
			: getForName(map.crs());
	}

	public static Res<Wsg84Transformer> getForName(String name) {
		if (name == null || name.isBlank())
			return Res.error("CRS name is null or blank");

		var crsFactory = new CRSFactory();
		CoordinateReferenceSystem target;
		try {
			target = crsFactory.createFromName("EPSG:4326");
		} catch (Exception e) {
			return Res.error("failed to create target CRS", e);
		}

		CoordinateReferenceSystem source;
		try {
			var n = mapNameOf(name);
			if (n == null)
				return Res.error("failed to determine source CRS: " + name);
			source = crsFactory.createFromName(n);
		} catch (Exception e) {
			return Res.error("failed to create source CRS", e);
		}

		var transformer = new CoordinateTransformFactory()
			.createTransform(source, target);
		return Res.of(new Wsg84Transformer(transformer));
	}

	private static String mapNameOf(String name) {
		if (name == null || name.isBlank())
			return null;
		if (name.startsWith("EPSG:"))
			return name;
		if (name.startsWith("urn:")) {
			var parts = name.split(":");
			return mapNameOf(parts[parts.length - 1]);
		}
		if (name.contains("*"))
			return mapNameOf(name.split("\\*")[0]);

		return switch (name.strip().toLowerCase()) {
			case "etrs89_utm32" -> "EPSG:25832";
			case "etrs89_utm33" -> "EPSG:25833";
			default -> name.strip();
		};
	}

	public ProjCoordinate[] exteriorRingOf(Polygon polygon) {
		if (polygon == null)
			return null;
		var ring = polygon.getExteriorRing();
		return ring != null
			? project(ring.getCoordinates())
			: null;
	}

	private GeometryFactory factory() {
		if (_factory == null) {
			_factory = new GeometryFactory(new PrecisionModel(), 4326);
		}
		return _factory;
	}

	public Polygon transform(Polygon polygon) {
		if (polygon == null)
			return null;
		var exterior = transformRing(polygon.getExteriorRing());
		if (exterior == null)
			return null;
		var holesCounts = polygon.getNumInteriorRing();
		if (holesCounts == 0)
			return factory().createPolygon(exterior);

		var holes = new LinearRing[holesCounts];
		for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
			holes[i] = transformRing(polygon.getInteriorRingN(i));
		}
		return factory().createPolygon(exterior, holes);
	}

	private LinearRing transformRing(LinearRing ring) {
		if (ring == null)
			return null;
		var cs = transform(ring.getCoordinates());
		return cs != null
			? factory().createLinearRing(cs)
			: null;
	}

	public Coordinate[] transform(Coordinate[] origin) {
		var pcs = project(origin);
		if (pcs == null)
			return null;
		var cs = new Coordinate[pcs.length];
		for (int i = 0; i < pcs.length; i++) {
			var pci = pcs[i];
			cs[i] = new Coordinate(pci.x, pci.y);
		}
		return cs;
	}

	private ProjCoordinate[] project(Coordinate[] cs) {
		if (cs == null || cs.length == 0)
			return null;
		var pcs = new ProjCoordinate[cs.length];
		for (int i = 0; i < cs.length; i++) {
			// Proj4j can handle in-place transformations (?)
			pcs[i] = new ProjCoordinate(cs[i].x, cs[i].y);
			fn.transform(pcs[i], pcs[i]);
		}
		return pcs;
	}
}
