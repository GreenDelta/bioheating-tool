package com.greendelta.bioheating.calc;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.operation.distance.DistanceOp;

import com.greendelta.bioheating.io.CrsId;
import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.GeoMap;
import com.greendelta.bioheating.model.Street;
import com.greendelta.bioheating.util.Res;
import com.greendelta.bioheating.util.Strings;

public class GeometryBuilder {

	private final GeometryFactory factory;

	private GeometryBuilder(GeometryFactory factory) {
		this.factory = factory;
	}

	public static GeometryBuilder getDefault() {
		return new GeometryBuilder(new GeometryFactory());
	}

	public static GeometryBuilder of(GeoMap map) {
		if (map == null || Strings.isNil(map.crs()))
			return getDefault();
		var crs = CrsId.parse(map.crs());
		if (!crs.isValid())
			return getDefault();
		var factory = new GeometryFactory(
			new PrecisionModel(), crs.code());
		return new GeometryBuilder(factory);
	}

	public Res<BuildingPolygon> polygonOf(Building building) {
		if (building == null || building.coordinates() == null)
			return Res.error("no building coordinates");
		try {
			var polygon = factory.createPolygon(building.coordinates());
			return Res.of(new BuildingPolygon(building, polygon));
		} catch (Exception e) {
			return Res.error("failed to create polygon", e);
		}
	}

	public Res<StreetLine> lineOf(Street street) {
		if (street == null || street.coordinates() == null)
			return Res.error("no street coordinates");
		try {
			var line = factory.createLineString(street.coordinates());
			return Res.of(new StreetLine(street, line));
		} catch (Exception e) {
			return Res.error("failed to create line-string", e);
		}
	}

	public Res<Connector> connectorOf(BuildingPolygon bp, StreetLine sl) {
		if (bp == null || sl == null)
			return Res.error("building polygon or street line is null");
		try {
			var points = DistanceOp.nearestPoints(bp.polygon(), sl.line());
			if (points.length < 2)
				return Res.error("failed to calculate distance: less than 2 points");
			var line = factory.createLineString(points);
			var con = new Connector(bp, sl, line, line.getLength());
			return Res.of(con);
		} catch (Exception e) {
			return Res.error("failed to calculate connector", e);
		}
	}
}
