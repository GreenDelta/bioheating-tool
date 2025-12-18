package com.greendelta.bioheating.graph;

import java.util.ArrayList;

import com.greendelta.bioheating.graph.HeatFlowTree.Junction;
import com.greendelta.bioheating.graph.HeatFlowTree.Segment;

/// Applies diversity factors to heat demands in a heat flow tree.
/// The diversity factor accounts for the fact that not all consumers
/// use their maximum heat demand simultaneously.
public class Diversity {

	private Diversity() {
	}

	/// Applies diversity factors to the given tree and returns a new tree
	/// with adjusted heat demands.
	public static HeatFlowTree apply(HeatFlowTree tree) {
		if (tree == null || tree.root() == null)
			return tree;
		var root = copyTree(tree.root());
		aggregateWithDiversity(root);
		return new HeatFlowTree(root);
	}

	private static Junction copyTree(Junction node) {
		var copy = new Junction(node.node());
		for (var seg : node.segments()) {
			var targetCopy = copyTree(seg.target());
			copy.segments().add(new Segment(seg.length(), targetCopy));
		}
		return copy;
	}

	private static Result aggregateWithDiversity(Junction junction) {
		int consumers = 0;
		double rawDemand = 0.0;

		var building = junction.building();
		if (building != null && building.heatDemand() > 0) {
			consumers = 1;
			rawDemand = building.heatDemand();
		}

		for (var seg : junction.segments()) {
			var result = aggregateWithDiversity(seg.target());
			consumers += result.consumers;
			rawDemand += result.rawDemand;

			// apply diversity factor to segment demand
			double diversifiedDemand = result.rawDemand * factor(result.consumers);
			seg.heatDemand(diversifiedDemand);
		}

		// apply diversity factor to junction demand
		double diversifiedDemand = rawDemand * factor(consumers);
		junction.heatDemand(diversifiedDemand);

		return new Result(consumers, rawDemand);
	}

	/// Calculates the diversity factor for n consumers.
	/// f(n) = 0.449677646267461 + (0.551234688 / (1 + (n / 53.84382392) ^ 1.762743268))
	public static double factor(int n) {
		if (n <= 0)
			return 1.0;
		if (n == 1)
			return 1.0;
		double ratio = n / 53.84382392;
		double power = Math.pow(ratio, 1.762743268);
		return 0.449677646267461 + (0.551234688 / (1 + power));
	}

	private record Result(int consumers, double rawDemand) {
	}
}
