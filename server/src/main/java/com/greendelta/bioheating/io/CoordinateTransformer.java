package com.greendelta.bioheating.io;

import java.util.Objects;

import org.locationtech.jts.geom.Coordinate;
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

	private CoordinateTransformer(CoordinateTransform fn) {
		this.fn = Objects.requireNonNull(fn);
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
		return Res.of(new CoordinateTransformer(transform));
	}

	private static Res<CoordinateReferenceSystem> crsOf(
		CrsId id, CRSFactory factory
	) {
		if (id == null)
			return Res.error("CRS ID is null");
		try {
			var crs = factory.createFromName(id.value());
			return Res.of(crs);
		} catch (Exception e) {
			return Res.error("could not create CRS " + id.value(), e);
		}
	}

	public Res<Coordinate[]> transform(Coordinate[] origin) {
		var res = project(origin);
		if (res.hasError())
			return res.castError();
		var pcs = res.value();
		var cs = new Coordinate[pcs.length];
		for (int i = 0; i < pcs.length; i++) {
			var pci = pcs[i];
			cs[i] = new Coordinate(pci.x, pci.y);
		}
		return Res.of(cs);
	}

	private Res<ProjCoordinate[]> project(Coordinate[] cs) {
		if (cs == null || cs.length == 0)
			return Res.error("no coordinates provided");
		try {
			var pcs = new ProjCoordinate[cs.length];
			for (int i = 0; i < cs.length; i++) {
				// Proj4j can handle in-place transformations (?)
				pcs[i] = new ProjCoordinate(cs[i].x, cs[i].y);
				fn.transform(pcs[i], pcs[i]);
			}
			return Res.of(pcs);
		} catch (Exception e) {
			return Res.error("coordinate transform failed", e);
		}
	}

	public Res<ProjCoordinate> project(double x, double y) {
		try {
			var pc = new ProjCoordinate(x, y);
			fn.transform(pc, pc);
			return Res.of(pc);
		} catch (Exception e) {
			return Res.error("coordinate transform failed", e);
		}
	}
}
