package com.greendelta.bioheating.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.index.strtree.ItemBoundable;
import org.locationtech.jts.index.strtree.ItemDistance;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.openlca.commons.Res;

import com.greendelta.bioheating.graph.Node.BuildingNode;
import com.greendelta.bioheating.graph.Node.StreetNode;
import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.Inclusion;
import com.greendelta.bioheating.model.Project;

public class Graph extends DefaultUndirectedWeightedGraph<Node, Edge> {

	public Graph() {
		super(Edge.class);
	}

	public static Res<Graph> buildFrom(Project project) {

		// first check that we have a project with streets and buildings
		if (project == null || project.map() == null)
			return Res.error("empty project provided");
		if (project.map().streets().isEmpty())
			return Res.error("project does not contain any streets");
		int bCount = 0;
		for (var b : project.map().buildings()) {
			if (isIncluded(b)) {
				bCount++;
			}
		}
		if (bCount == 0)
			return Res.error("Project does not contain any heated buildings");

		// try to build the graph
		try {
			var g = new Builder(project).build();
			return Res.ok(g);
		} catch (Exception e) {
			return Res.error("Failed to create graph", e);
		}

	}

	private static boolean isIncluded(Building b) {
		return b != null &&
			(b.isSupplyCenter() || (b.isHeated() && b.isIncluded()));
	}

	public void add(GraphPath<Node, Edge> path) {
		if (path == null)
			return;
		for (var edge : path.getEdgeList()) {
			add(edge);
		}
	}

	void add(Edge edge) {
		if (edge == null)
			return;
		addVertex(edge.source());
		addVertex(edge.target());
		var b = addEdge(edge.source(), edge.target(), edge);
		if (b) {
			setEdgeWeight(edge, edge.length());
		}
	}

	private static class Builder {

		private final Project project;
		private final AtomicLong ids = new AtomicLong(0);

		private final GeometryFactory factory = new GeometryFactory();
		private final Graph graph = new Graph();
		private final Map<Long, StreetNode> streetNodes = new HashMap<>();
		private final List<Edge> streets = new ArrayList<>();
		private final Map<Long, List<Edge>> streetParts = new HashMap<>();

		Builder(Project project) {
			this.project = project;
			long maxId = 0;
			for (var b : project.map().buildings()) {
				maxId = Math.max(maxId, b.id());
			}
			ids.set(maxId);
		}

		Graph build() {
			var tree = indexStreets();
			linkBuildings(tree);

			for (var s : streets) {
				var parts = streetParts.get(s.id());
				if (parts == null || parts.isEmpty()) {
					graph.add(s);
					continue;
				}
				for (var p : parts) {
					graph.add(p);
				}
			}

			return graph;
		}

		/// Creates the nodes and edges for the street segments and creates
		/// a search index for these segments.
		private STRtree indexStreets() {
			var tree = new STRtree();
			for (var s : project.map().streets()) {
				if (s.inclusion() == Inclusion.EXCLUDED)
					continue;

				var cs = s.coordinates();
				if (cs == null || cs.length < 2)
					continue;
				for (int i = 1; i < cs.length; i++) {
					var start = cs[i - 1];
					var end = cs[i];

					var source = getOrCreateNode(start);
					var target = getOrCreateNode(end);
					if (source.equals(target))
						continue;

					var line = factory.createLineString(new Coordinate[]{start, end});
					var edge = new Edge(
						ids.incrementAndGet(), source, target, line, line.getLength());
					streets.add(edge);
					tree.insert(line.getEnvelopeInternal(), edge);
				}
			}
			tree.build();
			return tree;
		}

		private StreetNode getOrCreateNode(Coordinate coo) {
			long x = Math.round(coo.x);
			long y = Math.round(coo.y);

			// check if there is an existing street node within a 1-meter distance
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					var existing = streetNodes.get(gridKey(x + dx, y + dy));
					if (existing != null && isClose(coo, existing))
						return existing;
				}
			}

			var point = factory.createPoint(coo);
			var newNode = new StreetNode(ids.incrementAndGet(), point);
			streetNodes.put(gridKey(x, y), newNode);
			graph.addVertex(newNode);
			return newNode;
		}

		/// An UTM coordinate rounded to full metres can be stored without data loss
		/// in two 32-bit segments in a 64-bit number.
		private long gridKey(long x, long y) {
			return (x << 32) | (y & 0xFFFFFFFFL);
		}

		/// Create the building nodes and link them with the street net. This will
		/// create connections from the buildings to the closest streets that are
		/// directly added to the graph. When adding the buildings, street segments
		/// may are split into several parts. The final street segments are added
		/// to the graph when this is finished.
		private void linkBuildings(STRtree tree) {

			var distanceFunc = new DistanceFunction();

			for (var b : project.map().buildings()) {
				if (!isIncluded(b))
					continue;

				var buildingNode = BuildingNode.of(b, factory);
				var n = tree.nearestNeighbour(
					buildingNode.envelope(), buildingNode.polygon(), distanceFunc);
				if (!(n instanceof Edge street))
					continue;

				graph.addVertex(buildingNode);
				var cs = DistanceOp.nearestPoints(buildingNode.polygon(), street.line());
				var streetNode = connectionPointOf(cs[1], street);
				var line = factory.createLineString(cs);
				var connection = new Edge(
					ids.incrementAndGet(),
					buildingNode,
					streetNode,
					line,
					line.getLength());
				graph.add(connection);
			}
		}

		private Node connectionPointOf(Coordinate coo, Edge street) {
			if (isClose(coo, street.source()))
				return street.source();
			if (isClose(coo, street.target()))
				return street.target();

			var parts = streetParts.get(street.id());
			var node = splitParts(coo, parts);
			if (node != null)
				return node;

			var p = edgePointOf(coo, street);
			if (p.hasEdges()) {
				parts = new ArrayList<>();
				parts.add(p.edge1);
				parts.add(p.edge2);
				streetParts.put(street.id(), parts);
			}
			return p.node;
		}

		private Node splitParts(Coordinate coo, List<Edge> parts) {
			if (parts == null || parts.isEmpty())
				return null;

			var point = factory.createPoint(coo);
			Edge part = null;
			double distance = Double.MAX_VALUE;
			for (var p : parts) {
				double d = point.distance(p.line());
				if (d < distance) {
					part = p;
					distance = d;
				}
			}

			if (part == null)
				return null;
			var p = edgePointOf(coo, part);
			if (!p.hasEdges())
				return p.node;

			parts.remove(part);
			parts.add(p.edge1);
			parts.add(p.edge2);
			return p.node;
		}

		private EdgePoint edgePointOf(Coordinate coo,  Edge edge) {
			if (isClose(coo, edge.source()))
				return EdgePoint.of(edge.source());
			if (isClose(coo, edge.target()))
				return EdgePoint.of(edge.target());

			var point = factory.createPoint(coo);
			var node = new StreetNode(ids.incrementAndGet(), point);
			var line1 = factory.createLineString(
				new Coordinate[]{edge.source().center().getCoordinate(), coo}
			);
			var edge1 = new Edge(
				ids.incrementAndGet(), edge.source(), node, line1, line1.getLength());

			var line2 = factory.createLineString(
				new Coordinate[]{coo, edge.target().center().getCoordinate()}
			);
			var edge2 = new Edge(
				ids.incrementAndGet(), node, edge.target(), line2, line2.getLength());
			return new EdgePoint(node, edge1, edge2);
		}

		private boolean isClose(Coordinate coo, Node node) {
			return coo.distance(node.center().getCoordinate()) < 1;
		}
	}

	private record EdgePoint(Node node, Edge edge1, Edge edge2) {

		static EdgePoint of(Node node) {
			return new EdgePoint(node, null, null);
		}

		boolean hasEdges() {
			return edge1 != null && edge2 != null;
		}
	}

	private static class DistanceFunction implements ItemDistance {

		@Override
		public double distance(ItemBoundable i, ItemBoundable j) {
			var gi = geometryOf(i);
			var gj = geometryOf(j);
			return gi.distance(gj);
		}

		private Geometry geometryOf(ItemBoundable i) {
			var item = i.getItem();
			return switch (item) {
				case Edge e -> e.line();
				case BuildingNode n -> n.polygon();
				case StreetNode n -> n.center();
				case Geometry g -> g;
				case null, default -> throw new RuntimeException(
					"Unexpected object type for distance calculation: " + item);
			};
		}
	}
}
