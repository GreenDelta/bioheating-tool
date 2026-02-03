package com.greendelta.bioheating.graph;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.Solution;
import com.greendelta.bioheating.model.SolutionEdge;
import com.greendelta.bioheating.model.SolutionNode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.openlca.commons.Res;

/// The network tree derived from a solution. The root of the tree is the
/// supply-center, heated buildings are leaves and street nodes are the inner
/// nodes of the tree.
public record NetworkTree(Junction root) {
	/// Creates a tree from the given solution. The solution must be persisted (so
	/// that nodes and edges have unique IDs) before calling this method (or
	/// `Solution.withTransientIds` must be called before).
	public static Res<NetworkTree> of(Solution solution) {
		if (solution == null) return Res.error("No solution provided");

		// find the supply center
		Junction root = null;
		for (var n : solution.nodes()) {
			var b = n.building();
			if (b != null && b.isSupplyCenter()) {
				root = new Junction(n);
				break;
			}
		}
		if (root == null) return Res.error("No supply center found in solution");

		// build the edge index
		var edges = new HashMap<Long, List<SolutionEdge>>();
		for (var e : solution.edges()) {
			edges.computeIfAbsent(e.source().id(), $ -> new ArrayList<>()).add(e);
			edges.computeIfAbsent(e.target().id(), $ -> new ArrayList<>()).add(e);
		}

		// build the tree
		var visited = new HashSet<Long>();
		visited.add(root.id());
		var queue = new ArrayDeque<Junction>();
		queue.add(root);
		while (!queue.isEmpty()) {
			var next = queue.poll();
			var exs = edges.get(next.id());
			if (exs == null) continue;
			for (var e : exs) {
				var t = Objects.equals(e.source(), next.node())
					? e.target()
					: e.source();
				if (visited.contains(t.id())) continue;
				var target = new Junction(t);
				var seg = new Segment(e, target);
				next.segments().add(seg);
				queue.add(target);
				visited.add(target.id());
			}
		}

		var tree = new NetworkTree(root);
		return Res.ok(tree);
	}

	/// A connection point of a street or building node of the
	/// network graph with (pipe) segments to other nodes.
	public record Junction(SolutionNode node, List<Segment> segments) {
		Junction(SolutionNode node) {
			this(node, new ArrayList<>());
		}

		public long id() {
			return node.id();
		}

		public Building building() {
			return node.building();
		}

		public boolean isBuilding() {
			return node.isBuildingNode();
		}
	}

	/// A pipe segment to a connection point in the network graph.
	public record Segment(SolutionEdge edge, Junction target) {
		public long id() {
			return edge.id();
		}

		public double length() {
			return edge.length();
		}
	}
}
