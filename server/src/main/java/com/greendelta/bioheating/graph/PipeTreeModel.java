package com.greendelta.bioheating.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.openlca.commons.Res;

import com.greendelta.bioheating.graph.HeatFlowTree.Junction;
import com.greendelta.bioheating.graph.HeatFlowTree.Segment;
import com.greendelta.bioheating.math.Thermo;
import com.greendelta.bioheating.model.Building;

public class PipeTreeModel {

	private HashMap<Long, PipeSegment> segments = new HashMap<>();

	public Res<PipeTreeModel> of(HeatFlowTree tree) {
		if (tree == null)
			return Res.error("No valid heat flow tree provided");
		try {
			var model = new PipeTreeModel();
			model.traverse(tree.root());
			return Res.ok(model);
		} catch (Exception e) {
			return Res.error("Failed to calculate pipe tree model", e);
		}
	}

	private PipeJunction traverse(Junction junction) {
		var segments = new ArrayList<PipeSegment>();
		for (var s : junction.segments()) {
			var target = s.target();
			if (target.isBuilding()) {
				segments.add(segmentOf(s, List.of(target.building())));
				continue;
			}
			var buildings = new ArrayList<Building>();
			for (var sub : traverse(s.target()).segments) {
				buildings.addAll(sub.buildings);
			}
			segments.add(segmentOf(s, buildings));
		}
		return new PipeJunction(segments);
	}

	private PipeSegment segmentOf(Segment s, List<Building> buildings) {
		double peakLoad = 0;
		for (var b : buildings) {
			peakLoad += b.peakLoad();
		}
		peakLoad *= Thermo.diversityFactorOf(buildings.size());
		var segment = new PipeSegment(s.id(), s.length(), peakLoad, buildings);
		segments.put(segment.id, segment);
		return segment;
	}

	public record PipeJunction(List<PipeSegment> segments) {
	}

	public record PipeSegment(
		long id,
		double length,
		double peakLoad,
		List<Building> buildings) {
	}
}
