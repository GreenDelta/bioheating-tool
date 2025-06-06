package com.greendelta.bioheating.citygml;

import java.util.Optional;

import org.citygml4j.core.model.core.CityModel;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.xmlobjects.gml.model.geometry.DirectPosition;

public record GmlEnvelope(
	String srs,
	int dimension,
	Point lowerCorner,
	Point upperCorner
) {

	public static Optional<GmlEnvelope> of(
		CityModel model, GeometryFactory factory
	) {
		if (model == null)
			return Optional.empty();
		var bounds = model.getBoundedBy();
		if (bounds == null)
			return Optional.empty();
		var env = bounds.getEnvelope();
		if (env == null)
			return Optional.empty();

		return Optional.of(new GmlEnvelope(
			env.getSrsName(),
			env.getDimension(),
			pointOf(env.getLowerCorner(), factory),
			pointOf(env.getUpperCorner(), factory)
		));
	}

	private static Point pointOf(DirectPosition pos, GeometryFactory factory) {
		if (pos == null || factory == null)
			return null;
		var coos = pos.getValue();
		if (coos == null || coos.size() < 2)
			return null;
		var x = coos.getFirst();
		var y = coos.get(1);
		var z = coos.size() > 2 ? coos.get(2) : null;
		if (x == null || y == null)
			return null;
		var c = z != null
			? new Coordinate(x, y, z)
			: new Coordinate(x, y);
		return factory.createPoint(c);
	}
}
