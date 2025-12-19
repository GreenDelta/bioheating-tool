package com.greendelta.bioheating.graph;

import java.util.ArrayList;
import java.util.List;

import com.greendelta.bioheating.model.Building;

/// A pipe plan derived from a heat flow tree. This class traverses the tree
/// bottom-up, collecting information about buildings and heat demands in
/// the subtree below each node.
public class PipePlan {

	private final HeatFlowTree tree;
	private final SegmentPlan rootPlan;

	private PipePlan(HeatFlowTree tree, SegmentPlan rootPlan) {
		this.tree = tree;
		this.rootPlan = rootPlan;
	}

	public static PipePlan of(HeatFlowTree tree) {
		if (tree == null || tree.root() == null)
			return null;
		var rootPlan = traverse(tree.root());
		return new PipePlan(tree, rootPlan);
	}

	public HeatFlowTree tree() {
		return tree;
	}

	public SegmentPlan rootPlan() {
		return rootPlan;
	}

	/// Traverses the tree bottom-up starting from the given junction.
	/// Returns a SegmentPlan that contains the aggregated information
	/// of the subtree rooted at this junction.
	private static SegmentPlan traverse(HeatFlowTree.Junction junction) {
		var plan = new SegmentPlan(junction);

		// first, recursively process all child segments (bottom-up)
		for (var segment : junction.segments()) {
			var childPlan = traverse(segment, segment.target());
			plan.childPlans.add(childPlan);

			// aggregate information from child plans
			plan.totalHeatDemand += childPlan.totalHeatDemand;
			plan.totalPipeLength += childPlan.totalPipeLength;
			plan.buildings.addAll(childPlan.buildings);
		}

		// add this junction's building if present
		var building = junction.building();
		if (building != null) {
			plan.buildings.add(building);
			plan.totalHeatDemand += building.heatDemand();
		}

		return plan;
	}

	/// Traverses a segment and its target junction bottom-up.
	private static SegmentPlan traverse(
			HeatFlowTree.Segment segment,
			HeatFlowTree.Junction junction) {

		var plan = new SegmentPlan(segment, junction);

		// first, recursively process all child segments (bottom-up)
		for (var childSegment : junction.segments()) {
			var childPlan = traverse(childSegment, childSegment.target());
			plan.childPlans.add(childPlan);

			// aggregate information from child plans
			plan.totalHeatDemand += childPlan.totalHeatDemand;
			plan.totalPipeLength += childPlan.totalPipeLength;
			plan.buildings.addAll(childPlan.buildings);
		}

		// add this segment's length
		plan.totalPipeLength += segment.length();

		// add this junction's building if present
		var building = junction.building();
		if (building != null) {
			plan.buildings.add(building);
			plan.totalHeatDemand += building.heatDemand();
		}

		return plan;
	}

	/// Contains the planning information for a segment and the subtree below it.
	public static class SegmentPlan {

		private final HeatFlowTree.Segment segment;
		private final HeatFlowTree.Junction junction;
		private final List<SegmentPlan> childPlans;
		private final List<Building> buildings;
		private double totalHeatDemand;
		private double totalPipeLength;

		/// Constructor for the root junction (no segment leading to it).
		SegmentPlan(HeatFlowTree.Junction junction) {
			this.segment = null;
			this.junction = junction;
			this.childPlans = new ArrayList<>();
			this.buildings = new ArrayList<>();
		}

		/// Constructor for a segment with its target junction.
		SegmentPlan(HeatFlowTree.Segment segment, HeatFlowTree.Junction junction) {
			this.segment = segment;
			this.junction = junction;
			this.childPlans = new ArrayList<>();
			this.buildings = new ArrayList<>();
		}

		/// The segment leading to this junction, or null for the root.
		public HeatFlowTree.Segment segment() {
			return segment;
		}

		/// The junction at the end of this segment.
		public HeatFlowTree.Junction junction() {
			return junction;
		}

		/// The plans for the child segments below this one.
		public List<SegmentPlan> childPlans() {
			return childPlans;
		}

		/// All buildings in the subtree below (and including) this junction.
		public List<Building> buildings() {
			return buildings;
		}

		/// The total heat demand of all buildings in the subtree.
		public double totalHeatDemand() {
			return totalHeatDemand;
		}

		/// The total pipe length in the subtree below this segment.
		public double totalPipeLength() {
			return totalPipeLength;
		}

		/// Returns true if this is the root plan (no segment leading to it).
		public boolean isRoot() {
			return segment == null;
		}

		/// Returns true if this is a leaf (no child segments).
		public boolean isLeaf() {
			return childPlans.isEmpty();
		}

		/// The number of buildings in the subtree.
		public int buildingCount() {
			return buildings.size();
		}
	}
}
