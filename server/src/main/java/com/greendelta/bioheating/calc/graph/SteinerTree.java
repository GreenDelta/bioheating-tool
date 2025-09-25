package com.greendelta.bioheating.calc.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.SpanningTreeAlgorithm.SpanningTree;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.alg.spanning.KruskalMinimumSpanningTree;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;

import com.greendelta.bioheating.calc.graph.Node.BuildingNode;
import com.greendelta.bioheating.util.Res;

public class SteinerTree {

	private final Executor executor;

	public SteinerTree(Executor executor) {
		this.executor = executor;
	}

	public SteinerTree() {
		this(null);
	}

	public Res<SpanningTree<Edge>> compute(Graph g) {
		if (g == null || g.vertexSet().isEmpty()) {
			return Res.error("empty graph provided");
		}

		var terminals = g.vertexSet().stream()
			.filter(n -> n instanceof BuildingNode)
			.collect(Collectors.toSet());

		if (terminals.isEmpty()) {
			return Res.error("no building nodes found in graph");
		}

		if (terminals.size() == 1) {
			// Single terminal - return a tree with just that vertex
			var result = new DefaultUndirectedWeightedGraph<Node, Edge>(Edge.class);
			result.addVertex(terminals.iterator().next());
			return Res.of(new KruskalMinimumSpanningTree<>(result).getSpanningTree());
		}

		var shortestPaths = getManyToManyPaths(g, terminals);
		var closure = metricClosureOf(shortestPaths);
		var closureMST = closureTreeOf(closure);
		var subGraph = subGraphOf(g, closureMST, shortestPaths);

		return Res.of(new KruskalMinimumSpanningTree<>(subGraph).getSpanningTree());
	}

	private Map<Node, Map<Node, GraphPath<Node, Edge>>> getManyToManyPaths(
		Graph g, Set<Node> terminals
	) {
		var paths = new HashMap<Node, Map<Node, GraphPath<Node, Edge>>>();
		var dijkstra = new DijkstraShortestPath<>(g);

		for (var source : terminals) {
			var sourcePaths = new HashMap<Node, GraphPath<Node, Edge>>();
			for (var target : terminals) {
				if (!source.equals(target)) {
					var path = dijkstra.getPath(source, target);
					if (path != null) {
						sourcePaths.put(target, path);
					}
				}
			}
			paths.put(source, sourcePaths);
		}

		return paths;
	}

	private Graph metricClosureOf(Map<Node, Map<Node, GraphPath<Node, Edge>>> shortestPaths) {
		var closure = new Graph();

		// Add all terminal vertices
		for (var source : shortestPaths.keySet()) {
			closure.addVertex(source);
		}

		// Add edges with shortest path distances
		for (var source : shortestPaths.keySet()) {
			var sourcePaths = shortestPaths.get(source);
			for (var target : sourcePaths.keySet()) {
				if (!closure.containsEdge(source, target) && !closure.containsEdge(target, source)) {
					var path = sourcePaths.get(target);
					var edge = new Edge(
						System.nanoTime(), // Simple ID generation
						source,
						target,
						null, // Line geometry not needed for closure
						path.getWeight()
					);
					closure.addEdge(source, target, edge);
					closure.setEdgeWeight(edge, path.getWeight());
				}
			}
		}

		return closure;
	}

	private Graph closureTreeOf(Graph closure) {
		var mst = new KruskalMinimumSpanningTree<>(closure).getSpanningTree();
		var tree = new Graph();

		// Add all vertices
		for (var v : closure.vertexSet()) {
			tree.addVertex(v);
		}

		// Add MST edges
		for (var edge : mst.getEdges()) {
			tree.addEdge(closure.getEdgeSource(edge), closure.getEdgeTarget(edge), edge);
			tree.setEdgeWeight(edge, closure.getEdgeWeight(edge));
		}

		return tree;
	}

	private Graph subGraphOf(
		Graph originalGraph,
		Graph closureMST,
		Map<Node, Map<Node, GraphPath<Node, Edge>>> shortestPaths
	) {
		var subGraph = new Graph();
		var addedEdges = new HashSet<Edge>();

		// For each edge in the closure MST, add the corresponding shortest path from original graph
		for (var closureEdge : closureMST.edgeSet()) {
			var source = closureMST.getEdgeSource(closureEdge);
			var target = closureMST.getEdgeTarget(closureEdge);

			// Find the shortest path between these terminals in the original graph
			var path = shortestPaths.get(source).get(target);
			if (path == null) {
				path = shortestPaths.get(target).get(source);
			}

			if (path != null) {
				// Add all vertices and edges from the path
				var pathVertices = path.getVertexList();
				for (var vertex : pathVertices) {
					subGraph.addVertex(vertex);
				}

				for (var edge : path.getEdgeList()) {
					if (!addedEdges.contains(edge)) {
						subGraph.addEdge(originalGraph.getEdgeSource(edge),
							originalGraph.getEdgeTarget(edge), edge);
						subGraph.setEdgeWeight(edge, originalGraph.getEdgeWeight(edge));
						addedEdges.add(edge);
					}
				}
			}
		}

		return subGraph;
	}
}
