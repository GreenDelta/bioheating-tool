package com.greendelta.bioheating.calc.graph;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.function.Function;

import javax.imageio.ImageIO;

import org.jgrapht.alg.interfaces.SpanningTreeAlgorithm.SpanningTree;
import org.locationtech.jts.geom.Envelope;

import com.greendelta.bioheating.calc.graph.Node.BuildingNode;
import com.greendelta.bioheating.io.GeoImage;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.model.Solution;
import com.greendelta.bioheating.model.SolutionEdge;
import com.greendelta.bioheating.model.SolutionNode;
import com.greendelta.bioheating.util.Res;

public record MinTreeSolution(Project project, SpanningTree<Edge> tree) {

	public Res<Solution> create() {

		var image = createImage();
		if (image.hasError())
			return image.wrapError("failed to create solution image");

		var solution = new Solution()
			.project(project)
			.image(image.value());

		var nodes = new HashMap<Long, SolutionNode>();
		Function<Node, SolutionNode> nodeFetch = n -> {
			var node = nodes.get(n.id());
			if (node != null)
				return node;
			node = new SolutionNode()
				.x(n.center().getX())
				.y(n.center().getY());
			if (n instanceof BuildingNode bn) {
				node.building(bn.building());
			}
			nodes.put(n.id(), node);
			solution.nodes().add(node);
			return node;
		};

		for (var e : tree.getEdges()) {
			var source = nodeFetch.apply(e.source());
			var target = nodeFetch.apply(e.target());
			var edge = new SolutionEdge()
				.source(source)
				.target(target)
				.length(e.length())
				.coordinates(e.line().getCoordinates());
			solution.edges().add(edge);
		}

		return Res.of(solution);
	}

	private Res<byte[]> createImage() {

		// calulate the envelope of the image
		var envelope = new Envelope();
		for (var street : project.map().streets()) {
			for (var c : street.coordinates()) {
				envelope.expandToInclude(c);
			}
		}

		try (var img = new GeoImage(1024, 800, envelope)) {

			// draw streets
			for (var street : project.map().streets()) {
				img.drawLine(street.coordinates(), Color.LIGHT_GRAY);
			}

			// draw buildings
			var buildingColor = new Color(216, 27, 96);
			for (var building : project.map().buildings()) {
				img.drawPolygon(building.coordinates(), buildingColor);
			}

			// draw minimal spanning tree edges in red
			for (var edge : tree.getEdges()) {
				img.draw(edge.line(), Color.RED);
			}

			// write the image
			try (var bos = new ByteArrayOutputStream()) {
				ImageIO.write(img.getImage(), "png", bos);
				return Res.of(bos.toByteArray());
			}
		} catch (Exception e) {
			return Res.error("failed to create the solution image", e);
		}
	}

}
