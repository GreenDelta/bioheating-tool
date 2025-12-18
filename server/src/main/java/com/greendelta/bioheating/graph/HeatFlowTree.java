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
	/// persisted (so that nodes have IDs) before calling this method (or
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
				var seg = new Segment(e.length(), target);
				next.segments().add(seg);
				queue.add(target);
				visited.add(target.id());
			}
		}

		// aggregate heat demands bottom-up
		aggregateDemands(root);

		var tree = new HeatFlowTree(root);
		return Res.ok(tree);
	}

	private static double aggregateDemands(Junction junction) {
		double demand = 0.0;
		var building = junction.building();
		if (building != null) {
			demand = building.heatDemand();
		}
		for (var seg : junction.segments()) {
			double segDemand = aggregateDemands(seg.target());
			seg.heatDemand(segDemand);
			demand += segDemand;
		}
		junction.heatDemand(demand);
		return demand;
	}

	/// Returns a compact version of this tree where linear paths of street nodes
	/// are aggregated into single segments.
	public HeatFlowTree compact() {
		var compactRoot = compact(root);
		aggregateDemands(compactRoot);
		return new HeatFlowTree(compactRoot);
	}

	private Junction compact(Junction node) {
		var compactNode = new Junction(node.node());
		for (var s : node.segments()) {
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
			compactNode.segments().add(new Segment(len, compactTarget));
		}
		return compactNode;
	}

	/// A connection point of a street or building node of the
	/// network graph with (pipe) segments to other nodes.
	public static class Junction {

		private final SolutionNode node;
		private final List<Segment> segments;
		private double heatDemand;

		Junction(SolutionNode node) {
			this.node = node;
			this.segments = new ArrayList<>();
		}

		public SolutionNode node() {
			return node;
		}

		public List<Segment> segments() {
			return segments;
		}

		public double heatDemand() {
			return heatDemand;
		}

		void heatDemand(double heatDemand) {
			this.heatDemand = heatDemand;
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
	public static class Segment {

		private final double length;
		private final Junction target;
		private double heatDemand;

		Segment(double length, Junction target) {
			this.length = length;
			this.target = target;
		}

		public double length() {
			return length;
		}

		public Junction target() {
			return target;
		}

		public double heatDemand() {
			return heatDemand;
		}

		void heatDemand(double heatDemand) {
			this.heatDemand = heatDemand;
		}
	}
}
