package com.greendelta.bioheating.calc.graph;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.jgrapht.alg.interfaces.SpanningTreeAlgorithm.SpanningTree;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

import com.greendelta.bioheating.calc.graph.Node.BuildingNode;
import com.greendelta.bioheating.io.GeoImage;
import com.greendelta.bioheating.model.GeoMap;
import com.greendelta.bioheating.model.Inclusion;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.util.Res;

class SolutionImage {

	private final GeoMap map;
	private final SpanningTree<Edge> tree;
	private final Envelope envelope;

	private final Color DISABLED_COLOR = new Color(144, 164, 174);
	private final Color PIPE_COLOR = new Color(69, 39, 160);
	private final Color BORDER_COLOR = new Color(55, 71, 79);

	private final Color PINK_1 = new Color(248, 187, 208);
	private final Color PINK_2 = new Color(244, 143, 177);
	private final Color PINK_3 = new Color(240, 98, 146);
	private final Color PINK_4 = new Color(236, 64, 122);
	private final Color PINK_5 = new Color(216, 27, 96);
	private final Color PINK_6 = new Color(194, 24, 91);
	private final Color PINK_7 = new Color(173, 20, 87);
	private final Color PINK_8 = new Color(136, 14, 79);

	private SolutionImage(Project project, SpanningTree<Edge> tree) {
		this.map = project.map();
		this.tree = tree;
		this.envelope = createEnvelope(tree);
	}

	private static Envelope createEnvelope(SpanningTree<Edge> tree) {
		var env = new Envelope();
		for (var edge : tree.getEdges()) {
			expand(env, edge.source());
			expand(env, edge.target());
		}
		return env;
	}

	private static void expand(Envelope env, Node node) {
		if (node instanceof BuildingNode n) {
			env.expandToInclude(n.envelope());
		} else {
			var center = node.center();
			if (center != null) {
				env.expandToInclude(center.getCoordinate());
			}
		}
	}

	static Res<byte[]> create(Project project, SpanningTree<Edge> tree) {
		if (project == null || project.map() == null || tree == null)
			return Res.error("empty project or solution");
		try {
			return new SolutionImage(project, tree).create();
		} catch (Exception e) {
			return Res.error("failed to create solution image", e);
		}
	}

	private Res<byte[]> create() {
		try (var img = new GeoImage(1024, 800, envelope)) {
			renderStreets(img);
			renderPipes(img);
			renderBuildings(img);

			try (var bos = new ByteArrayOutputStream()) {
				ImageIO.write(img.getImage(), "png", bos);
				return Res.of(bos.toByteArray());
			}
		} catch (Exception e) {
			return Res.error("failed to create the solution image", e);
		}
	}

	private void renderStreets(GeoImage img) {
		for (var street : map.streets()) {
			var cs = street.coordinates();
			if (cs == null || cs.length < 2)
				continue;
			for (int i = 1; i < cs.length; i++) {
				var start = cs[i-1];
				var end = cs[i];
				if (envelope.contains(start) && envelope.contains(end)) {
					img.drawLine(new Coordinate[]{start, end}, DISABLED_COLOR);
				}
			}
		}
	}

	private void renderPipes(GeoImage img) {
		for (var edge : tree.getEdges()) {
			var line = edge.line();
			if (line == null)
				continue;
			img.draw(line, PIPE_COLOR);
		}
	}

	private void renderBuildings(GeoImage img) {
		double maxDemand = 0;
		for (var e : tree.getEdges()) {
			if (e.target() instanceof BuildingNode n) {
				maxDemand = Math.max(maxDemand, n.building().heatDemand());
			}
			if (e.source() instanceof BuildingNode n) {
				maxDemand = Math.max(maxDemand, n.building().heatDemand());
			}
		}

		for (var e : tree.getEdges()) {
			if (e.target() instanceof BuildingNode n) {
				renderBuilding(img, n, maxDemand);
			}
			if (e.source() instanceof BuildingNode n) {
				renderBuilding(img, n, maxDemand);
			}
		}
	}

	private void renderBuilding(GeoImage img, BuildingNode n, double maxDemand) {
		var b = n.building();
		if (b == null)
			return;
		if (!b.isHeated() || b.inclusion() != Inclusion.REQUIRED || maxDemand == 0) {
			img.draw(n.polygon(), BORDER_COLOR, DISABLED_COLOR);
			return;
		}

		int share = (int) (Math.round(10 * b.heatDemand() / maxDemand));
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
		img.draw(n.polygon(), BORDER_COLOR, color);
	}

}
