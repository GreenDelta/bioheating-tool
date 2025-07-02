package com.greendelta.bioheating.calc;

import java.util.ArrayList;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.alg.spanning.KruskalMinimumSpanningTree;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import com.greendelta.bioheating.util.Res;

public class SteinerTree<V> {

	private final Graph<V, DefaultWeightedEdge> graph;
	private final Set<V> terminals;

	private SteinerTree(Graph<V, DefaultWeightedEdge> graph, Set<V> terminals) {
		this.graph = graph;
		this.terminals = terminals;
	}

	public static <V> Res<Graph<V, DefaultWeightedEdge>> compute(
		Graph<V, DefaultWeightedEdge> graph, Set<V> terminals
	) {
		return graph == null || terminals == null || terminals.isEmpty()
			? Res.error("empty graph or no terminals provided")
			: new SteinerTree<>(graph, terminals).compute();
	}

	private Res<Graph<V, DefaultWeightedEdge>> compute() {
		if (terminals.size() == 1) {
			var g = newGraph();
			g.addVertex(terminals.iterator().next());
			return Res.of(g);
		}

		var metricClosure = metricClosure();
		var metricTree = minTreeOf(metricClosure);

		return null;
	}


	/// Compute the metric closure of the terminal nodes.
	private Graph<V, DefaultWeightedEdge> metricClosure() {
		var closure = newGraph();
		for (var t : terminals) {
			closure.addVertex(t);
		}

		var pathFn = new DijkstraShortestPath<>(graph);
		var ts = new ArrayList<>(terminals);
		for (int i = 0; i < ts.size(); i++) {
			for (int j = i + 1; j < ts.size(); j++) {
				var ti = ts.get(i);
				var tj = ts.get(j);
				var path = pathFn.getPath(ti, tj);
				if (path == null)
					continue;
				var edge = closure.addEdge(ti, tj);
				if (edge != null) {
					closure.setEdgeWeight(edge, path.getWeight());
				}
			}
		}
		return closure;
	}

	private  Graph<V, DefaultWeightedEdge> minTreeOf(
		Graph<V, DefaultWeightedEdge> graph
	) {
		var edges = 	new KruskalMinimumSpanningTree<>(graph)
			.getSpanningTree()
			.getEdges();
		var mst = newGraph();
		for (var v : graph.vertexSet()) {
			mst.addVertex(v);
		}
		for (var e : edges) {
			var vi = graph.getEdgeSource(e);
			var vj = graph.getEdgeTarget(e);
			var edge = mst.addEdge(vi, vj);
			if (edge != null) {
				mst.setEdgeWeight(edge, graph.getEdgeWeight(e));
			}
		}
		return mst;
	}


	private Graph<V, DefaultWeightedEdge> newGraph() {
		return new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
	}
}
