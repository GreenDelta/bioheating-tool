package com.greendelta.bioheating.citygml;

import java.util.List;

import org.citygml4j.core.model.building.Building;
import org.citygml4j.core.model.construction.GroundSurface;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.xmlobjects.gml.model.geometry.aggregates.MultiSurface;
import org.xmlobjects.gml.model.geometry.primitives.AbstractRingProperty;
import org.xmlobjects.gml.model.geometry.primitives.Polygon;

import com.greendelta.bioheating.util.Res;

class PolygonReader {

	private final GeometryFactory factory = new GeometryFactory();

	Res<org.locationtech.jts.geom.Polygon> read(Building building) {
		var cs3d = coosOf(building);
		if (cs3d == null)
			return Res.error("could not get coordinates of building");
		int n = cs3d.size() / 3;
		var coordinates = new Coordinate[n];
		for (int i = 0; i < n; i++) {
			var x = cs3d.get(i * 3);
			var y = cs3d.get(i * 3 + 1);
			var z = cs3d.get(i * 3 + 2);
			coordinates[i] = new Coordinate(x, y, z);
		}
		var ring = factory.createLinearRing(coordinates);
		var polygon =  factory.createPolygon(ring);
		return Res.of(polygon);
	}

	private static List<Double> coosOf(Building b) {
		if (b == null)
			return null;
		for (var bound : b.getBoundaries()) {
			if (bound.getObject() instanceof GroundSurface gs) {
				var coos = groundCoosOf(gs);
				if (coos != null)
					return coos;
			}
		}
		return null;
	}

	private static List<Double> groundCoosOf(GroundSurface gs) {
		if (gs == null)
			return null;
		for (int i = 0; i < 4; i++) {
			var prop = gs.getMultiSurface(i);
			if (prop != null) {
				var cs = surfaceCoosOf(prop.getObject());
				if (cs != null)
					return cs;
			}
		}
		return null;
	}

	private static List<Double> surfaceCoosOf(MultiSurface ms) {
		if (ms == null)
			return null;
		for (var member : ms.getSurfaceMember()) {
			if (member.getObject() instanceof Polygon p) {
				var cs = polygonCoosOf(p);
				if (cs != null)
					return cs;
			}
		}
		return null;
	}

	private static List<Double> polygonCoosOf(Polygon p) {
		if (p == null)
			return null;
		var cs = ringCoosOf(p.getExterior());
		if (cs != null)
			return cs;
		for (var hole : p.getInterior()) {
			cs = ringCoosOf(hole);
			if (cs != null)
				return cs;
		}
		return null;
	}

	private static List<Double> ringCoosOf(AbstractRingProperty ring) {
		return ring != null && ring.getObject() != null
			? ring.getObject().toCoordinateList3D()
			: null;
	}
}

