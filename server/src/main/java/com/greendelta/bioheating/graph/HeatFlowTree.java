package com.greendelta.bioheating.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.openlca.commons.Res;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.Solution;
import com.greendelta.bioheating.model.SolutionEdge;
import com.greendelta.bioheating.model.SolutionNode;


/// The heat flow tree derived from a solution tree. The root of the tree
/// is the supply-center, heated buildings are leaves and street nodes
// are the inner nodes of the tree.
public record HeatFlowTree(Junction root) {

	public static Res<HeatFlowTree> of(Solution solution) {
		if (solution == null)
			return Res.error("No solution provided");

		// find the supply center
		Junction root = null;
		for (var n : solution.nodes()) {
			var b = n.building();
			if (b != null && b.isSupplyCenter()) {
				root = Junction.of(n);
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
				var target = Junction.of(t);
				var seg = new Segment(e.length(), target);
				next.segments.add(seg);
				queue.add(target);
				visited.add(target.id());
			}
		}

		var tree = new HeatFlowTree(root);
		return Res.ok(tree);
	}


	public String toDot() {
		var sb = new StringBuilder();
		sb.append("digraph CalcTree {\n");

		var ids = new IdentityHashMap<Junction, String>();
		var seq = new AtomicInteger(0);
		writeDot(root, sb, ids, seq);

		sb.append("}\n");
		return sb.toString();
	}


	private static void writeDot(
		Junction node,
		StringBuilder sb,
		IdentityHashMap<Junction, String> ids,
		AtomicInteger seq
	) {
		if (node == null)
			return;

		var id = ids.computeIfAbsent(node, k -> "n" + seq.incrementAndGet());
		var label = labelOf(node.node);
		var shape = node.node != null && node.node.isBuildingNode()
			? "box"
			: "ellipse";

		sb.append("  ").append(id)
			.append(" [shape=").append(shape)
			.append(", label=\"")
			.append(escape(label))
			.append("\"];\n");

		for (var child : node.segments) {
			writeDot(child.target, sb, ids, seq);
			var childId = ids.get(child);
			sb.append("  ").append(id)
				.append(" -> ")
				.append(childId)
				.append(";\n");
		}
	}

	private static String labelOf(SolutionNode node) {
		if (node == null)
			return "null";
		if (node.isBuildingNode()) {
			var b = node.building();
			var name = b != null && b.name() != null && !b.name().isBlank()
				? b.name()
				: "building";
			var flags = new ArrayList<String>();
			if (b != null && b.isSupplyCenter())
				flags.add("supply");
			if (b != null && b.isHeated())
				flags.add("heated");
			var suffix = flags.isEmpty()
				? ""
				: " (" + String.join(",", flags) + ")";
			var bid = b != null ? b.id() : 0;
			return name + " [bId=" + bid + "]" + suffix;
		}
		return "street [x=" + node.x() + ", y=" + node.y() + "]";
	}

	private static String escape(String s) {
		if (s == null)
			return "";
		return s.replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("\n", "\\n");
	}

	/// A connection point of a street or building node of the
	/// network graph with (pipe) segments to other nodes.
	public record Junction(
		SolutionNode node, List<Segment> segments) {

		static Junction of(SolutionNode n) {
			return new Junction(n, new ArrayList<>());
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
	public record Segment(double length, Junction target) {
	}
}
