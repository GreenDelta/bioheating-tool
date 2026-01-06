package com.greendelta.bioheating.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.index.strtree.STRtree;
import org.openlca.commons.Res;

import com.greendelta.bioheating.graph.Node.StreetNode;

record StreetNet(List<Edge> edges, STRtree index) {

	static Res<StreetNet> create(GraphConfig config) {
		try {
			var net = new Builder(config).build();
			return Res.ok(net);
		} catch (Exception e) {
			return Res.error("Failed to connect the streets to a graph", e);
		}
	}

	private static class Builder {

		private final GraphConfig config;
		private final List<Edge> edges = new ArrayList<>();
		private final Map<Long, StreetNode> nodes = new HashMap<>();
		private final List<StreetNode> allNodes = new ArrayList<>();
		private final STRtree index = new STRtree();

		private Builder(GraphConfig config) {
			this.config = config;
		}

		private StreetNet build() {
			createEdges();
			ensureConnected();
			index.build();
			return new StreetNet(edges, index);
		}

		private void createEdges() {
			for (var s : config.streets()) {
				if (s.isExcluded())
					continue;

				var cs = s.coordinates();
				if (cs == null || cs.length < 2)
					continue;
				for (int i = 1; i < cs.length; i++) {
					var start = cs[i - 1];
					var end = cs[i];

					var source = nodeOf(start);
					var target = nodeOf(end);
					if (source.equals(target))
						continue;

					var line = config.lineOf(start, end);
					var edge = config.edgeOf(source, target, line);
					edges.add(edge);
					index.insert(edge.envelope(), edge);
				}
			}
		}

		private StreetNode nodeOf(Coordinate coo) {
			long x = Math.round(coo.x);
			long y = Math.round(coo.y);

			// check if there is an existing street node within a 1-meter distance
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					var existing = nodes.get(gridKey(x + dx, y + dy));
					if (existing != null && config.isClose(coo, existing))
						return existing;
				}
			}

			var point = config.pointOf(coo);
			var newNode = new StreetNode(config.nextId(), point);
			nodes.put(gridKey(x, y), newNode);
			allNodes.add(newNode);
			return newNode;
		}

		/// An UTM coordinate rounded to full metres can be stored without data loss
		/// in two 32-bit segments in a 64-bit number.
		private long gridKey(long x, long y) {
			return (x << 32) | (y & 0xFFFFFFFFL);
		}

		private void ensureConnected() {
			if (edges.isEmpty() || allNodes.size() < 2)
				return;

			// add all connected node sets
			var uf = new UnionFind();
			for (var n : allNodes) {
				uf.add(n.id());
			}
			for (var e : edges) {
				uf.union(e.source().id(), e.target().id());
			}

			// find the connected components
			var components = new HashMap<Long, List<StreetNode>>();
			for (var n : allNodes) {
				var root = uf.find(n.id());
				components
					.computeIfAbsent(root, _k -> new ArrayList<>())
					.add(n);
			}
			if (components.size() <= 1)
				return;

			// select the largest component as the main component
			Long mainRoot = null;
			int maxSize = -1;
			for (var entry : components.entrySet()) {
				int size = entry.getValue().size();
				if (size > maxSize) {
					maxSize = size;
					mainRoot = entry.getKey();
				}
			}
			if (mainRoot == null)
				return;

			// connect the components
			var mainNodes = components.remove(mainRoot);
			for (var otherNodes : components.values()) {
				var pair = closestPair(mainNodes, otherNodes);
				if (pair == null)
					continue;
				var mainNode = pair[0];
				var otherNode = pair[1];
				var line = config.lineOf(
					mainNode.center().getCoordinate(),
					otherNode.center().getCoordinate());
				var connector = config.edgeOf(mainNode, otherNode, line);
				edges.add(connector);
				index.insert(connector.envelope(), connector);
				mainNodes.addAll(otherNodes);
			}
		}

		private StreetNode[] closestPair(
			List<StreetNode> mainNodes, List<StreetNode> otherNodes
		) {
			if (mainNodes == null || mainNodes.isEmpty())
				return null;
			if (otherNodes == null || otherNodes.isEmpty())
				return null;

			StreetNode bestMain = null;
			StreetNode bestOther = null;
			double bestDistance = Double.MAX_VALUE;
			for (var o : otherNodes) {
				for (var m : mainNodes) {
					double d = o.center().distance(m.center());
					if (d < bestDistance) {
						bestDistance = d;
						bestMain = m;
						bestOther = o;
					}
				}
			}
			return bestMain != null
				? new StreetNode[]{bestMain, bestOther}
				: null;
		}

		private static class UnionFind {

			private final Map<Long, Long> parent = new HashMap<>();
			private final Map<Long, Integer> rank = new HashMap<>();

			void add(long x) {
				parent.putIfAbsent(x, x);
				rank.putIfAbsent(x, 0);
			}

			long find(long x) {
				Long p = parent.get(x);
				if (p == null) {
					add(x);
					return x;
				}
				if (p != x) {
					long root = find(p);
					parent.put(x, root);
					return root;
				}
				return x;
			}

			void union(long a, long b) {
				long rootA = find(a);
				long rootB = find(b);
				if (rootA == rootB)
					return;

				int rankA = rank.getOrDefault(rootA, 0);
				int rankB = rank.getOrDefault(rootB, 0);
				if (rankA < rankB) {
					parent.put(rootA, rootB);
				} else if (rankB < rankA) {
					parent.put(rootB, rootA);
				} else {
					parent.put(rootB, rootA);
					rank.put(rootA, rankA + 1);
				}
			}
		}
	}
}
