package com.greendelta.bioheating.graph;

/// Visualization utilities for the heat flow tree.
public class HeatFlowViz {

	private final HeatFlowTree tree;
	private final StringBuilder sb;
	private final double total;

	private HeatFlowViz(HeatFlowTree tree) {
		this.tree = tree;
		this.sb = new StringBuilder();
		this.total = tree.root().heatDemand();
	}

	public static String toDot(HeatFlowTree tree) {
		if (tree == null || tree.root() == null)
			return "";
		return new HeatFlowViz(tree).build();
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
		double size = nodeSizeOf(node.heatDemand());

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
			double edgeWidth = edgeWidthOf(seg.heatDemand());
			sb.append("  ").append(id)
				.append(" -> ")
				.append(childId)
				.append(" [penwidth=")
				.append(String.format("%.2f", edgeWidth))
				.append("];\n");
		}
	}

	private double nodeSizeOf(double demand) {
		if (total <= 0)
			return 0.1;
		double ratio = demand / total;
		return 0.1 + ratio * 0.4;
	}

	private double edgeWidthOf(double demand) {
		if (total <= 0)
			return 0.5;
		double ratio = demand / total;
		return 0.5 + ratio * 8;
	}
}
