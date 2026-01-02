package com.greendelta.bioheating.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.openlca.commons.Res;

import com.greendelta.bioheating.graph.HeatFlowTree.Junction;
import com.greendelta.bioheating.graph.HeatFlowTree.Segment;
import com.greendelta.bioheating.math.PipeConfig;
import com.greendelta.bioheating.math.Thermo;
import com.greendelta.bioheating.model.Building;

public class PipePlan {

	private final HashMap<Long, PipeSegment> segments = new HashMap<>();
	private final HashMap<Long, PipeJunction> junctions = new HashMap<>();

	private PipePlan() {
	}

	public static Res<PipePlan> of(HeatFlowTree tree) {
		if (tree == null)
			return Res.error("No valid heat flow tree provided");
		try {
			var model = new PipePlan();
			model.traverse(tree.root());
			return Res.ok(model);
		} catch (Exception e) {
			return Res.error("Failed to calculate pipe tree model", e);
		}
	}

	public double peakLoadOf(Segment segment) {
		if (segment == null)
			return 0;
		var s = segments.get(segment.id());
		return s != null ? s.peakLoad : 0;
	}

	public double pressureLossOf(
		Segment segment, PipeConfig config, double diameter) {
		double load = peakLoadOf(segment);
		if (load <= 0)
			return 0;
		double temp = (config.flowTemperature() + config.returnTemperature()) / 2;
		double massFlow = Thermo.massFlowOf(
			config.flowTemperature(), config.returnTemperature(), load);
		double velocity = Thermo.flowVelocityOf(massFlow, diameter, temp);
		return Thermo.pressureLossOf(velocity, diameter, config.roughness(), temp);
	}

	public double peakLoadOf(Junction junction) {
		if (junction == null)
			return 0;
		var j = junctions.get(junction.id());
		return j != null ? j.peakLoad : 0;
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
		return junctionOf(junction, segments);
	}

	private PipeJunction junctionOf(Junction j, List<PipeSegment> segments) {
		int n = 0;
		double peakLoad = 0;
		for (var s : segments) {
			for (var b : s.buildings) {
				n += 1;
				peakLoad += b.peakLoad();
			}
		}
		peakLoad *= Thermo.diversityFactorOf(n);
		var junction = new PipeJunction(j.id(), peakLoad, segments);
		junctions.put(junction.id, junction);
		return junction;
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

	public record PipeJunction(
		long id, double peakLoad, List<PipeSegment> segments) {
	}

	public record PipeSegment(
		long id,
		double length,
		double peakLoad,
		List<Building> buildings) {
	}
}
