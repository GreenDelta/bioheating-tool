package com.greendelta.bioheating.graph;

import com.greendelta.bioheating.graph.Node.BuildingNode;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.jgrapht.alg.interfaces.ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths;
import org.jgrapht.alg.interfaces.SpanningTreeAlgorithm.SpanningTree;
import org.jgrapht.alg.shortestpath.CHManyToManyShortestPaths;
import org.jgrapht.alg.spanning.KruskalMinimumSpanningTree;
import org.openlca.commons.Res;

public class SteinerTree {

	private final ThreadPoolExecutor exec;

	public SteinerTree(ThreadPoolExecutor exec) {
		this.exec = exec;
	}

	public static Res<SpanningTree<Edge>> compute(
		ThreadPoolExecutor exec,
		Graph g
	) {
		if (exec == null) return Res.error("no executor provided");
		if (g == null) return Res.error("no graph provided");
		try {
			return new SteinerTree(exec).build(g);
		} catch (Exception e) {
			return Res.error("failed to calculate Steiner-Tree", e);
		}
	}

	public static Res<SpanningTree<Edge>> compute(Graph g) {
		var cpus = Runtime.getRuntime().availableProcessors();
		int n = switch (cpus) {
			case 0, 1, 2 -> 1;
			case 3, 4, 5 -> 2;
			default -> cpus / 2;
		};

		// same what Executors.newFixedThreadPool(n) would do but we
		// need a ThreadPoolExecutor, and newFixedThreadPool just returns
		// an ExecutorService
		var exec = new ThreadPoolExecutor(
			n,
			n,
			0L,
			TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<>()
		);
		try (exec) {
			return compute(exec, g);
		} catch (Exception e) {
			return Res.error("failed to calculate Steiner-Tree", e);
		}
	}

	private Res<SpanningTree<Edge>> build(Graph g) {
		if (g == null || g.vertexSet().isEmpty()) return Res.error(
			"empty graph provided"
		);

		var terminals = g
			.vertexSet()
			.stream()
			.filter(n -> n instanceof BuildingNode)
			.collect(Collectors.toSet());
		if (terminals.isEmpty()) return Res.error(
			"no building nodes found in graph"
		);

		if (terminals.size() == 1) {
			var result = new Graph();
			result.addVertex(terminals.iterator().next());
			return Res.ok(mstOf(result));
		}

		var paths = new CHManyToManyShortestPaths<>(g, exec).getManyToManyPaths(
			terminals,
			terminals
		);
		var closure = metricClosureOf(terminals, paths);
		var closureTree = mstOf(closure);
		var subGraph = subGraphOf(closureTree, paths);
		return Res.ok(mstOf(subGraph));
	}

	private SpanningTree<Edge> mstOf(Graph g) {
		return new KruskalMinimumSpanningTree<>(g).getSpanningTree();
	}

	private Graph metricClosureOf(
		Set<Node> terminals,
		ManyToManyShortestPaths<Node, Edge> paths
	) {
		long maxId = 0;
		for (var n : terminals) {
			maxId = Math.max(maxId, n.id());
		}
		var ids = new AtomicLong(maxId);

		var closure = new Graph();
		for (var i : terminals) {
			for (var j : terminals) {
				if (i.equals(j)) continue;
				var path = paths.getPath(i, j);
				if (path == null) continue;
				var edge = new Edge(
					ids.incrementAndGet(),
					i,
					j,
					null,
					path.getWeight()
				);
				closure.add(edge);
			}
		}
		return closure;
	}

	private Graph subGraphOf(
		SpanningTree<Edge> closureTree,
		ManyToManyShortestPaths<Node, Edge> paths
	) {
		var g = new Graph();
		for (var e : closureTree.getEdges()) {
			var i = e.source();
			var j = e.target();
			var path = paths.getPath(i, j);
			if (path == null) continue;
			for (var edge : path.getEdgeList()) {
				g.add(edge);
			}
		}
		return g;
	}
}
