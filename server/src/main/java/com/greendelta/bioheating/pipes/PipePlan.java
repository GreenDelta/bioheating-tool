package com.greendelta.bioheating.pipes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.openlca.commons.Res;

import com.greendelta.bioheating.graph.HeatFlowTree;
import com.greendelta.bioheating.graph.HeatFlowTree.Junction;
import com.greendelta.bioheating.graph.HeatFlowTree.Segment;
import com.greendelta.bioheating.model.Building;

public class PipePlan {

	private final PipeConfig config;
	private final List<Pipe> pipes;
	private final HashMap<Long, PipeSegment> segments = new HashMap<>();
	private final HashMap<Long, PipeJunction> junctions = new HashMap<>();

	private PipePlan(PipeConfig config) {
		this.config = config;
		this.pipes = config.pipes();
	}

	public static Res<PipePlan> of(PipeConfig config, HeatFlowTree tree) {
		if (config == null)
			return Res.error("No configuration provided");
		if (tree == null)
			return Res.error("No valid heat flow tree provided");
		try {
			var model = new PipePlan(config);
			var result = model.traverse(tree.root());
			return result.isError()
				? result.wrapError("Failed to calculate pipe-plan")
				: Res.ok(model);
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

	public double peakLoadOf(Junction junction) {
		if (junction == null)
			return 0;
		var j = junctions.get(junction.id());
		if (j != null)
			return j.peakLoad;
		return junction.isBuilding()
			? junction.building().peakLoad()
			: 0;
	}

	public Pipe pipeOf(Segment segment) {
		if (segment == null)
			return null;
		var s = segments.get(segment.id());
		return s != null ? s.pipe : null;
	}

	private Res<PipeJunction> traverse(Junction junction) {
		var segments = new ArrayList<PipeSegment>();
		for (var s : junction.segments()) {
			var target = s.target();
			if (target.isBuilding()) {
				var segment = segmentOf(s, List.of(target.building()));
				if (segment.isError())
					return segment.castError();
				segments.add(segment.value());
				continue;
			}
			var subJunction = traverse(s.target());
			if (subJunction.isError())
				return subJunction.castError();
			var buildings = new ArrayList<Building>();
			for (var sub : subJunction.value().segments) {
				buildings.addAll(sub.buildings);
			}
			var segment = segmentOf(s, buildings);
			if (segment.isError())
				return segment.castError();
			segments.add(segment.value());
		}
		return junctionOf(junction, segments);
	}

	private Res<PipeJunction> junctionOf(Junction j, List<PipeSegment> segments) {
		int n = 0;
		double peakLoad = 0;
		for (var s : segments) {
			for (var b : s.buildings) {
				n += 1;
				peakLoad += b.peakLoad();
			}
		}
		peakLoad *= Pipes.diversityFactorOf(n);
		var junction = new PipeJunction(j.id(), peakLoad, segments);
		junctions.put(junction.id, junction);
		return Res.ok(junction);
	}

	private Res<PipeSegment> segmentOf(Segment s, List<Building> buildings) {
		// peakLoad in kW
		double peakLoad = 0;
		for (var b : buildings) {
			peakLoad += b.peakLoad();
		}
		peakLoad *= Pipes.diversityFactorOf(buildings.size());

		// temperature difference in K (°C difference equals K difference)
		double deltaT = config.averageTemperature() - config.groundTemperature();

		Pipe pipe = null;
		double segmentLoad = 0;
		for (var p : pipes) {
			// pipe heat loss: Q_loss = U * L * ΔT
			// U in W/(m·K), length in m, ΔT in K => pipeLoss in W
			// convert to kW by dividing by 1000
			double pipeLoss = p.uValue() * s.length() * deltaT / 1000;

			// totalLoad in kW (peakLoad in kW + pipeLoss in kW)
			double totalLoad = peakLoad + pipeLoss;

			// inner diameter in m (converted from mm)
			double di = p.innerDiameter() / 1000;
			double massFlow = Pipes.massFlowOf(
				config.flowTemperature(), config.returnTemperature(), totalLoad);
			double velocity = Pipes.flowVelocityOf(
				massFlow, di, config.averageTemperature());
			if (velocity > config.maxFlowVelocity())
				continue;
			var pressureLoss = Pipes.pressureLossOf(
				velocity, di, config.roughness(), config.averageTemperature())
				* (1 + config.fittingSurcharge());
			if (pressureLoss < config.maxPressureLoss()) {
				pipe = p;
				segmentLoad = totalLoad;
				break;
			}
		}

		if (pipe == null) {
			return Res.error("No suitable pipe found for segment " + s.id()
				+ " with peak load " + peakLoad + " W");
		}

		var segment = new PipeSegment(
			s.id(), s.length(), segmentLoad, pipe, buildings);
		segments.put(segment.id, segment);
		return Res.ok(segment);
	}

	public record PipeJunction(
		long id, double peakLoad, List<PipeSegment> segments) {
	}

	public record PipeSegment(
		long id,
		double length,
		double peakLoad,
		Pipe pipe,
		List<Building> buildings) {
	}
}
