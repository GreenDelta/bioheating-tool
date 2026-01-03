package com.greendelta.bioheating.graph;

import com.greendelta.bioheating.graph.HeatFlowTree.Junction;
import com.greendelta.bioheating.graph.HeatFlowTree.Segment;
import com.greendelta.bioheating.pipes.PipePlan;

/// Visualization utilities for the heat flow tree.
public class HeatFlowViz {

	private final HeatFlowTree tree;
	private final PipePlan model;
	private final StringBuilder sb;
	private final double total;

	private HeatFlowViz(HeatFlowTree tree, PipePlan model) {
		this.tree = tree;
		this.model = model;
		this.sb = new StringBuilder();
		this.total = model.peakLoadOf(tree.root());
	}

	public static String toDot(HeatFlowTree tree) {
		if (tree == null || tree.root() == null)
			return "";
		var model = PipePlan.of(tree);
		if (model.isError())
			return "Error: " + model.error();
		return new HeatFlowViz(tree, model.value()).build();
	}

	private String build() {
		sb.append("digraph {\n");
		sb.append("  node [style=filled, fillcolor=black];\n");
		sb.append("  edge [dir=none];\n");
		writeDot(tree.root());
		sb.append("}\n");
		return sb.toString();
	}

	private void writeDot(HeatFlowTree.Junction node) {
		if (node == null)
			return;

		var id = Long.toString(node.id());
		boolean isBuilding = node.isBuilding();
		double size = nodeSizeOf(node);

		if (isBuilding) {
			// buildings (leaves and root) are squares
			sb.append("  ").append(id)
				.append(" [shape=square, label=\"\", width=")
				.append(String.format("%.2f", size))
				.append(", height=")
				.append(String.format("%.2f", size))
				.append("];\n");
		} else {
			// street nodes (inner nodes) are filled circles
			sb.append("  ").append(id)
				.append(" [shape=circle, label=\"\", width=")
				.append(String.format("%.2f", size))
				.append(", height=")
				.append(String.format("%.2f", size))
				.append("];\n");
		}

		for (var seg : node.segments()) {
			writeDot(seg.target());
			var childId = Long.toString(seg.target().id());
			double edgeWidth = edgeWidthOf(seg);
			sb.append("  ").append(id)
				.append(" -> ")
				.append(childId)
				.append(" [penwidth=")
				.append(String.format("%.2f", edgeWidth))
				.append("];\n");
		}
	}

	private double nodeSizeOf(Junction j) {
		if (total <= 0)
			return 0.1;
		double ratio = model.peakLoadOf(j) / total;
		return 0.1 + ratio * 0.4;
	}

	private double edgeWidthOf(Segment s) {
		if (total <= 0)
			return 0.5;
		double ratio = model.peakLoadOf(s) / total;
		return 0.5 + ratio * 8;
	}
}
