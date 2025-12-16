package com.greendelta.bioheating.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.openlca.commons.Res;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.Solution;
import com.greendelta.bioheating.model.SolutionEdge;
import com.greendelta.bioheating.model.SolutionNode;

/**
 * A rooted calculation tree derived from a solution tree.
 *
 * Root is the supply-center building node. Heated buildings are leaves; street
 * nodes are inner nodes.
 */
public final class HeatFlowTree {

	private final HeatFlowNode root;

	private HeatFlowTree(HeatFlowNode root) {
		this.root = root;
	}

	public HeatFlowNode root() {
		return root;
	}

	public static Res<HeatFlowTree> of(Solution solution) {
		if (solution == null)
			return Res.error("solution is null");
		if (solution.nodes() == null || solution.nodes().isEmpty())
			return Res.error("solution has no nodes");
		if (solution.edges() == null || solution.edges().isEmpty())
			return Res.error("solution has no edges");

		var root = supplyCenterOf(solution.nodes());
		if (root == null)
			return Res.error("solution has no supply-center building node");

		var adjacency = adjacencyOf(solution.edges());
		var built = build(root, adjacency);
		if (built == null)
			return Res.error("failed to build calc tree");

		prune(built, true);
		return Res.ok(new HeatFlowTree(built));
	}

	private static SolutionNode supplyCenterOf(List<SolutionNode> nodes) {
		for (var n : nodes) {
			if (n == null || !n.isBuildingNode())
				continue;
			var b = n.building();
			if (b != null && b.isSupplyCenter())
				return n;
		}
		return null;
	}

	private static Map<SolutionNode, List<SolutionNode>> adjacencyOf(
		List<SolutionEdge> edges
	) {
		var adjacency = new HashMap<SolutionNode, List<SolutionNode>>();
		for (var e : edges) {
			if (e == null || e.source() == null || e.target() == null)
				continue;
			adjacency.computeIfAbsent(e.source(), k -> new ArrayList<>())
				.add(e.target());
			adjacency.computeIfAbsent(e.target(), k -> new ArrayList<>())
				.add(e.source());
		}
		return adjacency;
	}

	private static HeatFlowNode build(
		SolutionNode root,
		Map<SolutionNode, List<SolutionNode>> adjacency
	) {
		var visited = new HashSet<SolutionNode>();
		var rootNode = new HeatFlowNode(root);
		visited.add(root);

		var queue = new ArrayDeque<HeatFlowNode>();
		queue.add(rootNode);

		while (!queue.isEmpty()) {
			var parent = queue.poll();
			var neighbors = adjacency.get(parent.data);
			if (neighbors == null || neighbors.isEmpty())
				continue;
			for (var n : neighbors) {
				if (n == null || visited.contains(n))
					continue;
				visited.add(n);
				var child = new HeatFlowNode(n);
				parent.children.add(child);
				queue.add(child);
			}
		}

		return rootNode;
	}

	private static boolean prune(HeatFlowNode node, boolean isRoot) {
		if (node == null)
			return false;

		for (int i = node.children.size() - 1; i >= 0; i--) {
			var child = node.children.get(i);
			if (!prune(child, false)) {
				node.children.remove(i);
			}
		}

		if (isRoot)
			return true;

		if (isHeatedBuilding(node.data))
			return true;

		return !node.children.isEmpty();
	}

	private static boolean isHeatedBuilding(SolutionNode node) {
		if (node == null || !node.isBuildingNode())
			return false;
		var b = node.building();
		return b != null && b.isHeated();
	}

	public String toDot() {
		var sb = new StringBuilder();
		sb.append("digraph CalcTree {\n");

		var ids = new IdentityHashMap<HeatFlowNode, String>();
		var seq = new AtomicInteger(0);
		writeDot(root, sb, ids, seq);

		sb.append("}\n");
		return sb.toString();
	}

	private static void writeDot(
		HeatFlowNode node,
		StringBuilder sb,
		IdentityHashMap<HeatFlowNode, String> ids,
		AtomicInteger seq
	) {
		if (node == null)
			return;

		var id = ids.computeIfAbsent(node, k -> "n" + seq.incrementAndGet());
		var label = labelOf(node.data);
		var shape = node.data != null && node.data.isBuildingNode()
			? "box"
			: "ellipse";

		sb.append("  ").append(id)
			.append(" [shape=").append(shape)
			.append(", label=\"")
			.append(escape(label))
			.append("\"];\n");

		for (var child : node.children) {
			writeDot(child, sb, ids, seq);
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
		return "street [x=" + round(node.x()) + ", y=" + round(node.y()) + "]";
	}

	private static String escape(String s) {
		if (s == null)
			return "";
		return s.replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("\n", "\\n");
	}

	private static String round(double v) {
		return String.format(java.util.Locale.ROOT, "%.2f", v);
	}

	public static final class HeatFlowNode {
		private final SolutionNode data;
		private final List<HeatFlowNode> children = new ArrayList<>();

		private HeatFlowNode(SolutionNode data) {
			this.data = Objects.requireNonNull(data);
		}

		public SolutionNode data() {
			return data;
		}

		public List<HeatFlowNode> children() {
			return children;
		}

		public Building building() {
			return data.building();
		}

		public boolean isBuildingNode() {
			return data.isBuildingNode();
		}
	}
}
