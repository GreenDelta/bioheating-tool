package com.greendelta.bioheating.graph;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.greendelta.bioheating.model.SolutionNode;

/// Visualization utilities for the heat flow tree.
public class HeatFlowViz {

	public static String toDot(HeatFlowTree tree) {
		if (tree == null || tree.root() == null)
			return "";
		var sb = new StringBuilder();
		sb.append("digraph CalcTree {\n");

		var ids = new IdentityHashMap<HeatFlowTree.Junction, String>();
		var seq = new AtomicInteger(0);
		writeDot(tree.root(), sb, ids, seq);

		sb.append("}\n");
		return sb.toString();
	}

	private static void writeDot(
		HeatFlowTree.Junction node,
		StringBuilder sb,
		IdentityHashMap<HeatFlowTree.Junction, String> ids,
		AtomicInteger seq
	) {
		if (node == null)
			return;

		var id = ids.computeIfAbsent(node, k -> "n" + seq.incrementAndGet());
		var label = labelOf(node.node());
		var shape = node.node() != null && node.node().isBuildingNode()
			? "box"
			: "ellipse";

		sb.append("  ").append(id)
			.append(" [shape=").append(shape)
			.append(", label=\"")
			.append(escape(label))
			.append("\"];\n");

		for (var child : node.segments()) {
			writeDot(child.target(), sb, ids, seq);
			// FIX: get the id of the target junction, not the segment
			var childId = ids.get(child.target());
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
}
