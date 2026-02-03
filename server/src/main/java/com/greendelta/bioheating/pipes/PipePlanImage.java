package com.greendelta.bioheating.pipes;

import com.greendelta.bioheating.graph.NetworkTree;
import com.greendelta.bioheating.io.GeoImage;
import com.greendelta.bioheating.model.GeoMap;
import com.greendelta.bioheating.model.Solution;
import com.greendelta.bioheating.model.SolutionNode;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.openlca.commons.Res;

/// Creates an image visualization of a pipe plan solution.
public class PipePlanImage {

	private final Solution solution;
	private final GeoMap map;
	private final NetworkTree tree;
	private final PipePlan plan;
	private final Envelope envelope;
	private final Map<Long, Pipe> edgePipes;

	private final Color DISABLED_COLOR = new Color(144, 164, 174);
	private final Color BORDER_COLOR = new Color(55, 71, 79);

	private final Color SUPPLY_CENTER_COLOR = new Color(255, 87, 34);

	private final Color PINK_1 = new Color(248, 187, 208);
	private final Color PINK_2 = new Color(244, 143, 177);
	private final Color PINK_3 = new Color(240, 98, 146);
	private final Color PINK_4 = new Color(236, 64, 122);
	private final Color PINK_5 = new Color(216, 27, 96);
	private final Color PINK_6 = new Color(194, 24, 91);
	private final Color PINK_7 = new Color(173, 20, 87);
	private final Color PINK_8 = new Color(136, 14, 79);

	private PipePlanImage(Solution solution, NetworkTree tree, PipePlan plan) {
		this.solution = solution;
		this.map = solution.project().map();
		this.tree = tree;
		this.plan = plan;
		this.envelope = createEnvelope();
		this.edgePipes = buildEdgePipeMap();
	}

	/// Creates a pipe plan image from the given solution using the default
	/// plastic pipe configuration.
	public static Res<byte[]> create(Solution solution) {
		return create(solution, PipeConfig.forPlastic().get());
	}

	/// Creates a pipe plan image from the given solution using the given
	/// pipe configuration.
	public static Res<byte[]> create(Solution solution, PipeConfig config) {
		if (solution == null) return Res.error("No solution provided");
		if (
			solution.project() == null || solution.project().map() == null
		) return Res.error("Solution has no project or map");

		var treeRes = NetworkTree.of(solution.withTransientIds());
		if (treeRes.isError()) return treeRes.castError();

		var planRes = PipePlan.of(config, treeRes.value());
		if (planRes.isError()) return planRes.castError();

		try {
			return new PipePlanImage(
				solution,
				treeRes.value(),
				planRes.value()
			).render();
		} catch (Exception e) {
			return Res.error("Failed to create pipe plan image", e);
		}
	}

	private Envelope createEnvelope() {
		var env = new Envelope();
		for (var edge : solution.edges()) {
			expandWithNode(env, edge.source());
			expandWithNode(env, edge.target());
		}
		return env;
	}

	private void expandWithNode(Envelope env, SolutionNode node) {
		if (node == null) return;
		var b = node.building();
		if (b != null && b.coordinates() != null) {
			for (var c : b.coordinates()) {
				env.expandToInclude(c);
			}
		}
	}

	/// Maps edge IDs to their selected pipes by traversing the heat flow tree.
	private Map<Long, Pipe> buildEdgePipeMap() {
		var map = new HashMap<Long, Pipe>();
		traverseForPipes(tree.root(), map);
		return map;
	}

	private void traverseForPipes(
		NetworkTree.Junction junction,
		Map<Long, Pipe> map
	) {
		if (junction == null) return;
		for (var seg : junction.segments()) {
			var pipe = plan.pipeOf(seg);
			if (pipe != null) {
				map.put(seg.id(), pipe);
			}
			traverseForPipes(seg.target(), map);
		}
	}

	private Res<byte[]> render() {
		try (var img = new GeoImage(1024, envelope)) {
			renderStreets(img);
			renderPipes(img);
			renderBuildings(img);

			try (var bos = new ByteArrayOutputStream()) {
				ImageIO.write(img.getImage(), "png", bos);
				return Res.ok(bos.toByteArray());
			}
		} catch (Exception e) {
			return Res.error("Failed to render pipe plan image", e);
		}
	}

	private void renderStreets(GeoImage img) {
		for (var street : map.streets()) {
			var cs = street.coordinates();
			if (cs == null || cs.length < 2) continue;
			for (int i = 1; i < cs.length; i++) {
				var start = cs[i - 1];
				var end = cs[i];
				if (envelope.contains(start) && envelope.contains(end)) {
					img.drawLine(new Coordinate[] { start, end }, DISABLED_COLOR);
				}
			}
		}
	}

	private void renderPipes(GeoImage img) {
		var factory = new GeometryFactory();
		for (var edge : solution.edges()) {
			var coords = edge.coordinates();
			if (coords == null || coords.length < 2) continue;

			var pipe = edgePipes.get(edge.id());
			var color = pipe != null ? Color.decode(pipe.rgb()) : DISABLED_COLOR;

			LineString line = factory.createLineString(coords);
			img.draw(line, color, 3.0f);
		}
	}

	private void renderBuildings(GeoImage img) {
		double maxDemand = 0;
		for (var node : solution.nodes()) {
			var b = node.building();
			if (b != null) {
				maxDemand = Math.max(maxDemand, b.heatDemand());
			}
		}

		for (var node : solution.nodes()) {
			var b = node.building();
			if (b == null) continue;
			renderBuilding(img, node, maxDemand);
		}
	}

	private void renderBuilding(
		GeoImage img,
		SolutionNode node,
		double maxDemand
	) {
		var b = node.building();
		if (b == null || b.coordinates() == null) return;

		var factory = new GeometryFactory();
		var polygon = factory.createPolygon(b.coordinates());

		if (b.isSupplyCenter()) {
			img.draw(polygon, BORDER_COLOR, SUPPLY_CENTER_COLOR);
			return;
		}
		if (!b.isHeated() || !b.isIncluded() || maxDemand == 0) {
			img.draw(polygon, BORDER_COLOR, DISABLED_COLOR);
			return;
		}

		int share = (int) (Math.round((10 * b.heatDemand()) / maxDemand));
		var color = switch (share) {
			case 0, 1 -> PINK_1;
			case 2 -> PINK_2;
			case 3 -> PINK_3;
			case 4 -> PINK_4;
			case 5 -> PINK_5;
			case 6 -> PINK_6;
			case 7 -> PINK_7;
			default -> PINK_8;
		};
		img.draw(polygon, BORDER_COLOR, color);
	}
}
