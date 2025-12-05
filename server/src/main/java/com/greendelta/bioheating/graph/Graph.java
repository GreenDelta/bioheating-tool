package com.greendelta.bioheating.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.ItemBoundable;
import org.locationtech.jts.index.strtree.ItemDistance;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.openlca.commons.Res;

import com.greendelta.bioheating.graph.Node.BuildingNode;
import com.greendelta.bioheating.graph.Node.StreetNode;
import com.greendelta.bioheating.model.Project;

public class Graph extends DefaultUndirectedWeightedGraph<Node, Edge> {

	public Graph() {
		super(Edge.class);
	}

	public static Res<Graph> buildFrom(Project project) {
		try {
			var config = GraphConfig.of(project);
			if (config.isError())
				return config.wrapError("Failed to create graph configuration");

			var streetNet = StreetNet.create(config.value());
			if (streetNet.isError())
				return streetNet.wrapError("Failed to create street graph");

			var g = new Builder(config.value(), streetNet.value()).build();
			return Res.ok(g);
		} catch (Exception e) {
			return Res.error("Failed to create graph", e);
		}

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

		private final GraphConfig config;
		private final List<Edge> streets;
		private final STRtree streetIndex;

		private final Graph graph = new Graph();
		private final Map<Long, List<Edge>> streetParts = new HashMap<>();

		Builder(GraphConfig config, StreetNet streetNet) {
			this.config = config;
			this.streets = streetNet.edges();
			this.streetIndex = streetNet.index();
		}

		Graph build() {
			linkBuildings();
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

		/// Create the building nodes and link them with the street net. This will
		/// create connections from the buildings to the closest streets that are
		/// directly added to the graph. When adding the buildings, street segments
		/// may are split into several parts. The final street segments are added
		/// to the graph when this is finished.
		private void linkBuildings() {

			var distanceFunc = new DistanceFunction();
			config.eachIncludedBuilding(b -> {
				var node = BuildingNode.of(b, config.factory());
				var n = streetIndex.nearestNeighbour(
					node.envelope(), node.polygon(), distanceFunc);
				if (!(n instanceof Edge street))
					return;
				graph.addVertex(node);
				var cs = DistanceOp.nearestPoints(node.polygon(), street.line());
				var streetNode = connectionPointOf(cs[1], street);
				var line = config.lineOf(cs);
				var connection = config.edgeOf(node, streetNode,line);
				graph.add(connection);
			});

		}

		private Node connectionPointOf(Coordinate coo, Edge street) {
			if (config.isClose(coo, street.source()))
				return street.source();
			if (config.isClose(coo, street.target()))
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

			var point = config.pointOf(coo);
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
			if (config.isClose(coo, edge.source()))
				return EdgePoint.of(edge.source());
			if (config.isClose(coo, edge.target()))
				return EdgePoint.of(edge.target());

			var point = config.pointOf(coo);
			var node = new StreetNode(config.nextId(), point);
			var line1 = config.lineOf(edge.source().center().getCoordinate(), coo);
			var edge1 = config.edgeOf(edge.source(), node, line1);

			var line2 = config.lineOf(coo, edge.target().center().getCoordinate());
			var edge2 = config.edgeOf(node, edge.target(), line2);
			return new EdgePoint(node, edge1, edge2);
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
