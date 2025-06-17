package com.greendelta.bioheating.io;

import java.util.Objects;

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


public class CoordinateTransformer {

	private final CoordinateTransform fn;
	private final CrsId targetId;
	private GeometryFactory _factory;

	private CoordinateTransformer(CoordinateTransform fn,CrsId targetId) {
		this.fn = Objects.requireNonNull(fn);
		this.targetId = Objects.requireNonNull(targetId);
	}

	public static Res<CoordinateTransformer> toWgs84From(GeoMap map) {
		if (map == null)
			return Res.error("map is null");
		return Strings.isNil(map.crs())
			? Res.error("CRS of model is not defined")
			: toWgs84From(map.crs());
	}

	public static Res<CoordinateTransformer> toWgs84From(String sourceCrs) {
		return Strings.isNil(sourceCrs)
			? Res.error("empty ID of source CRS")
			: of(CrsId.parse(sourceCrs), CrsId.wgs84());
	}

	public static Res<CoordinateTransformer> fromWgs84To(String targetCrs) {
		return Strings.isNil(targetCrs)
			? Res.error("empty ID of target CRS")
			: of(CrsId.wgs84(), CrsId.parse(targetCrs));
	}

	public static Res<CoordinateTransformer> of(CrsId sourceId, CrsId targetId) {
		var factory = new CRSFactory();
		var source = crsOf(sourceId, factory);
		if (source.hasError())
			return source.wrapError("failed to create source CRS");
		var target = crsOf(targetId, factory);
		if (target.hasError())
			return target.wrapError("failed to create target CRS");
		var transform = new CoordinateTransformFactory()
			.createTransform(source.value(), target.value());
		return Res.of(new CoordinateTransformer(transform, targetId));
	}

	private static Res<CoordinateReferenceSystem> crsOf(CrsId id, CRSFactory factory) {
		if (id == null)
			return Res.error("CRS ID is null");
		try {
			var crs = factory.createFromName(id.value());
			return Res.of(crs);
		} catch (Exception e) {
			return Res.error("could not create CRS " + id.value(), e);
		}
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
			_factory = new GeometryFactory(new PrecisionModel(), targetId.code());
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
