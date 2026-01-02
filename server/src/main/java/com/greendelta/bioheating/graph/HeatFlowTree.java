package com.greendelta.bioheating.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import org.openlca.commons.Res;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.Solution;
import com.greendelta.bioheating.model.SolutionEdge;
import com.greendelta.bioheating.model.SolutionNode;


/// The heat flow tree derived from a solution tree. The root of the tree
/// is the supply-center, heated buildings are leaves and street nodes
/// are the inner nodes of the tree.
public record HeatFlowTree(Junction root) {

	/// Creates a heat flow tree from the given solution. The solution must be
	/// persisted (so that nodes have unique IDs) before calling this method (or
	/// `Solution.withTransientIds` must be called before).
	public static Res<HeatFlowTree> of(Solution solution) {
		if (solution == null)
			return Res.error("No solution provided");

		// find the supply center
		Junction root = null;
		for (var n : solution.nodes()) {
			var b = n.building();
			if (b != null && b.isSupplyCenter()) {
				root = new Junction(n);
				break;
			}
		}
		if (root == null)
			return Res.error("No supply center found in solution");

		// build the edge index
		var edges = new HashMap<Long, List<SolutionEdge>>();
		for (var e : solution.edges()) {
			edges
				.computeIfAbsent(e.source().id(), $ -> new ArrayList<>())
				.add(e);
			edges
				.computeIfAbsent(e.target().id(), $ -> new ArrayList<>())
				.add(e);
		}

		// build the tree
		var visited = new HashSet<Long>();
		visited.add(root.id());
		var queue = new ArrayDeque<Junction>();
		queue.add(root);
		while (!queue.isEmpty()) {
			var next = queue.poll();
			var exs = edges.get(next.id());
			if (exs == null)
				continue;
			for (var e : exs) {
				var t = Objects.equals(e.source(), next.node())
					? e.target()
					: e.source();
				if (visited.contains(t.id()))
					continue;
				var target = new Junction(t);
				var seg = new Segment(e.id(), e.length(), target);
				next.segments().add(seg);
				queue.add(target);
				visited.add(target.id());
			}
		}

		var tree = new HeatFlowTree(root);
		return Res.ok(tree);
	}

	/// Returns a compact version of this tree where linear paths of street nodes
	/// are aggregated into single segments.
	public HeatFlowTree compact() {
		var compactRoot = compact(root);
		return new HeatFlowTree(compactRoot);
	}

	private Junction compact(Junction node) {
		var compactNode = new Junction(node.node());
		for (var s : node.segments()) {
			long id = s.id();
			double len = s.length();
			var next = s.target();

			// traverse down as long as we have a linear path of street nodes
			while (!next.isBuilding() && next.segments().size() == 1) {
				var nextSeg = next.segments().getFirst();
				len += nextSeg.length();
				next = nextSeg.target();
			}

			// recursively compact the target node
			var compactTarget = compact(next);
			compactNode.segments().add(new Segment(id, len, compactTarget));
		}
		return compactNode;
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
	public record Segment(long id, double length, Junction target) {
	}
}
