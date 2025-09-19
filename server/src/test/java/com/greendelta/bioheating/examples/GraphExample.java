package com.greendelta.bioheating.examples;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.operation.distance.DistanceOp;

import com.greendelta.bioheating.Tests;
import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.model.Street;

public class GraphExample {

	public static void main(String[] args) {

		var factory = new GeometryFactory();
		try (var db = Tests.db()) {
			System.out.println("Load project");
			var project = db.getAll(Project.class).getFirst();

			System.out.println("Create street segments");
			var segments = new ArrayList<StreetSegment>();
			for (var street : project.map().streets()) {
				segments.addAll(StreetSegment.createFrom(street, factory));
			}
			System.out.println("  .. created " + segments.size() + " segments");

			System.out.println("Create building connectors");
			var connectors = new ArrayList<BuildingConnector>();
			for (var b : project.map().buildings()) {
				var shell = factory.createPolygon(b.coordinates());
				var center = shell.getCentroid();

				BuildingConnector connector = null;
				for (var segment : segments) {
					var pts = DistanceOp.nearestPoints(center, segment.line);
					var con = factory.createLineString(pts);
					var len = con.getLength();
					if (connector == null || len < connector.length()) {
						connector = new BuildingConnector(b, segment, con, len);
					}
				}
				if (connector != null) {
					connectors.add(connector);
				}
			}
			System.out.println("  .. created " + connectors.size() + " connectors");

		}
	}

	record Edge(Node<?> from, Node<?> to) {
	}

	record Node<E>(double x, double y, E owner) {

	}

	private record BuildingConnector(
		Building building,
		StreetSegment segment,
		LineString line,
		double length
	) {
	}

	private record StreetSegment(
		String id,
		Street street,
		LineString line,
		double length
	) {

		static List<StreetSegment> createFrom(Street street, GeometryFactory fac) {
			var cs = street.coordinates();
			if (cs.length < 2)
				return List.of();
			var segments = new ArrayList<StreetSegment>();
			for (int i = 1; i < cs.length; i++) {
				var line = fac.createLineString(new Coordinate[] {
					cs[i-1], cs[i]
				});
				segments.add(new StreetSegment(
					UUID.randomUUID().toString(),
					street,
					line,
					line.getLength()));
			}
			return segments;
		}
	}

	private class ClusterBuilder {

		private final HashSet<String> required;
		private final List<StreetSegment> segments;
		private final List<BuildingConnector> connectors;

		ClusterBuilder(
			List<BuildingConnector> connectors, List<StreetSegment> segments
		) {
			this.connectors = connectors;
			this.segments = segments;
			this.required = new HashSet<>();
			for (var con : connectors) {
				required.add(con.segment.id);
			}
		}

		void build() {
			var handled = new HashSet<String>();

			for (var connector : connectors) {

			}
		}

	}
}
