package com.greendelta.bioheating.pipes;

import com.greendelta.bioheating.graph.HeatFlowTree;
import com.greendelta.bioheating.graph.HeatFlowTree.Junction;
import com.greendelta.bioheating.graph.HeatFlowTree.Segment;

/// Visualization utilities for the heat flow tree.
public class PipePlanViz {

	private final HeatFlowTree tree;
	private final PipePlan plan;
	private final StringBuilder sb;
	private final double total;

	private PipePlanViz(HeatFlowTree tree, PipePlan plan) {
		this.tree = tree;
		this.plan = plan;
		this.sb = new StringBuilder();
		this.total = plan.peakLoadOf(tree.root());
	}

	public static String toDot(PipeConfig config, HeatFlowTree tree) {
		if (tree == null || tree.root() == null)
			return "";
		var plan = PipePlan.of(config, tree);
		if (plan.isError())
			return "Error: " + plan.error();
		return new PipePlanViz(tree, plan.value()).build();
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
			var pipe = plan.pipeOf(seg);
			var color = pipe != null ? pipe.rgb() : "#000000";
			var diameter = pipe != null
				? String.format("%.0f", pipe.innerDiameter())
				: "";
			sb.append("  ").append(id)
				.append(" -> ")
				.append(childId)
				.append(" [penwidth=")
				.append(String.format("%.2f", edgeWidth))
				.append(", color=\"")
				.append(color)
				.append("\", label=\"")
				.append(diameter)
				.append("\"];\n");
		}
	}

	private double nodeSizeOf(Junction j) {
		if (total <= 0)
			return 0.1;
		double ratio = plan.peakLoadOf(j) / total;
		return 0.1 + ratio * 0.4;
	}

	private double edgeWidthOf(Segment s) {
		if (total <= 0)
			return 0.5;
		double ratio = plan.peakLoadOf(s) / total;
		return 0.5 + ratio * 8;
	}
}
