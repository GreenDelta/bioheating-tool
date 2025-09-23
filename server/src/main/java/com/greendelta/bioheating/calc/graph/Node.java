package com.greendelta.bioheating.calc.graph;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import com.greendelta.bioheating.model.Building;

public sealed interface Node {

	long id();

	Point center();

	record BuildingNode(
		Building building, Polygon polygon, Envelope envelope, Point center
	) implements Node {

		public static BuildingNode of(Building building, GeometryFactory f) {
			var polygon = f.createPolygon(building.coordinates());
			var center = polygon.getCentroid();
			var envelope = polygon.getEnvelopeInternal();
			return new BuildingNode(building, polygon, envelope, center);
		}

		@Override
		public long id() {
			return building().id();
		}

		@Override
		public int hashCode() {
			return Long.hashCode(id());
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null)
				return false;
			if (obj == this)
				return true;
			if (!(obj instanceof BuildingNode other))
				return false;
			return id() == other.id();
		}
	}

	record StreetNode(long id, Point center) implements Node {

		@Override
		public int hashCode() {
			return Long.hashCode(id);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null)
				return false;
			if (obj == this)
				return true;
			if (!(obj instanceof StreetNode other))
				return false;
			return id() == other.id();
		}
	}

}
