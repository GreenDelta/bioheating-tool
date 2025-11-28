package com.greendelta.bioheating.calc.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.openlca.commons.Res;

import com.greendelta.bioheating.calc.graph.Node.BuildingNode;
import com.greendelta.bioheating.calc.graph.Node.StreetNode;
import com.greendelta.bioheating.model.Inclusion;
import com.greendelta.bioheating.model.Project;

public class Graph extends DefaultUndirectedWeightedGraph<Node, Edge> {

	public Graph() {
		super(Edge.class);
	}

	public static Res<Graph> buildFrom(Project project) {
		if (project == null || project.map() == null)
			return Res.error("empty project provided");
		if (project.map().streets().isEmpty())
			return Res.error("project does not contain any streets");
		int bCount = 0;
		for (var b : project.map().buildings()) {
			if (b.isHeated() && b.inclusion() != Inclusion.REQUIRED) {
				bCount++;
			}
		}
		if (bCount == 0)
			return Res.error("project does not contain any heated buildings");
		try {
			var g = new Builder(project).build();
			return Res.ok(g);
		} catch (Exception e) {
			return Res.error("failed to create graph", e);
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

		private final Project project;
		private final GeometryFactory factory;
		private final AtomicLong ids;
		private final Graph graph;
		private final List<StreetNode> streetNodes;

		Builder(Project project) {
			this.project = project;
			this.factory = new GeometryFactory();
			this.graph = new Graph();
			this.streetNodes = new ArrayList<>();

			this.ids = new AtomicLong(0);
			long maxId = 0;
			for (var b : project.map().buildings()) {
				maxId = Math.max(maxId, b.id());
			}
			ids.set(maxId);
		}

		Graph build() {
			var tree = indexStreets();
			joinBuildings(tree);
			return graph;
		}

		private STRtree indexStreets() {
			var tree = new STRtree();
			for (var s : project.map().streets()) {
				var cs = s.coordinates();
				if (cs == null || cs.length < 2)
					continue;
				for (int i = 1; i < cs.length; i++) {
					var start = cs[i - 1];
					var end = cs[i];

					var source = getOrCreateNode(start);
					var target = getOrCreateNode(end);

					// Skip if source and target are the same (would create a self-loop)
					if (source.equals(target))
						continue;

					var line = factory.createLineString(new Coordinate[]{start, end});
					var edge = new Edge(
						ids.incrementAndGet(), source, target, line, line.getLength());
					graph.add(edge);
					tree.insert(line.getEnvelopeInternal(), edge);
				}
			}
			return tree;
		}

		private StreetNode getOrCreateNode(Coordinate coo) {
			// find existing node within 1 meter
			for (var n : streetNodes) {
				if (coo.distance(n.center().getCoordinate()) < 1.0) {
					return n;
				}
			}
			var point = factory.createPoint(coo);
			var newNode = new StreetNode(ids.incrementAndGet(), point);
			streetNodes.add(newNode);
			return newNode;
		}

		private void joinBuildings(STRtree tree) {
			for (var b : project.map().buildings()) {
				if (!b.isHeated() || b.inclusion() != Inclusion.REQUIRED)
					continue;
				var node = BuildingNode.of(b, factory);
				graph.addVertex(node);
				var env = node.envelope().copy();
				List<Edge> edges = List.of();
				int i = 0;
				while (edges.isEmpty() && i < 100) {
					i++;
					env.expandBy(10);
					var rs = tree.query(env);
					if (rs == null || rs.isEmpty())
						continue;
					for (var obj : rs) {
						edges = new ArrayList<>(rs.size());
						if (obj instanceof Edge e) {
							edges.add(e);
						}
					}
				}

				for (var e : edges) {
					var cs = DistanceOp.nearestPoints(node.polygon(), e.line());
					var split = splitPointOf(cs[1], e);
					var line = factory.createLineString(cs);
					var edge = new Edge(
						ids.incrementAndGet(), node, split, line, line.getLength());
					graph.add(edge);
				}
			}
		}

		private Node splitPointOf(Coordinate cs, Edge edge) {
			if (isClose(cs, edge.source()))
				return edge.source();
			if (isClose(cs, edge.target()))
				return edge.target();

			var node = new StreetNode(ids.incrementAndGet(), factory.createPoint(cs));
			var line1 = factory.createLineString(
				new Coordinate[]{edge.source().center().getCoordinate(), cs}
			);
			var edge1 = new Edge(
				ids.incrementAndGet(), edge.source(), node, line1, line1.getLength());
			graph.add(edge1);

			var line2 = factory.createLineString(
				new Coordinate[]{cs, edge.target().center().getCoordinate()}
			);
			var edge2 = new Edge(
				ids.incrementAndGet(), node, edge.target(), line2, line2.getLength());
			graph.add(edge2);
			return node;
		}

		private static boolean isClose(Coordinate cs, Node node) {
			return cs.distance(node.center().getCoordinate()) < 1;
		}
	}
}
