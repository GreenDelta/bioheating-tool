package com.greendelta.bioheating.calc.graph;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import javax.imageio.ImageIO;

import org.jgrapht.alg.interfaces.SpanningTreeAlgorithm.SpanningTree;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.alg.spanning.KruskalMinimumSpanningTree;
import org.locationtech.jts.geom.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.greendelta.bioheating.calc.graph.Node.BuildingNode;
import com.greendelta.bioheating.io.GeoImage;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.model.Solution;
import com.greendelta.bioheating.model.SolutionEdge;
import com.greendelta.bioheating.model.SolutionNode;
import com.greendelta.bioheating.util.Res;

public class MinTree {

	private MinTree() {
	}

	public static Res<SpanningTree<Edge>> of(Project project) {
		return project != null
			? new Builder(project).build()
			: Res.error("no project provided");
	}

	public static Res<Solution> solutionOf(Project project) {
		var treeRes = of(project);
		if (treeRes.hasError())
			return treeRes.castError();
		var tree = treeRes.value();
		var image = imageOf(project, tree);
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

	private static Res<byte[]> imageOf(Project project, SpanningTree<Edge> tree) {

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

	private static class Builder {

		private Logger log = LoggerFactory.getLogger(getClass());
		private final Project project;

		Builder(Project project) {
			this.project = project;
		}

		Res<SpanningTree<Edge>> build() {
			log.info("create minimal spanning tree of project {}", project.id());
			try {

				log.info("build full graph");
				var g = Graph.buildFrom(project);
				log.info("created graph with {} nodes and {} edges",
					g.vertexSet().size(), g.edgeSet().size());

				log.info("select center node");
				var buildingNodes = g.vertexSet()
					.stream()
					.filter(BuildingNode.class::isInstance)
					.map(BuildingNode.class::cast)
					.toList();
				var cix = ThreadLocalRandom
					.current()
					.nextInt(buildingNodes.size());
				var center = buildingNodes.get(cix);
				log.info("selected center: {}", center.id());

				log.info("build minimal graph");
				var minGraph = new Graph();
				for (var target : buildingNodes) {
					if (target.equals(center))
						continue;
					var path = DijkstraShortestPath.findPathBetween(g, center, target);
					minGraph.add(path);
				}
				log.info("created graph with {} nodes and {} edges",
					minGraph.vertexSet().size(), minGraph.edgeSet().size());

				log.info("build minimal spanning tree");
				var tree = new KruskalMinimumSpanningTree<>(minGraph).getSpanningTree();
				log.info("created tree with {} edges", tree.getEdges().size());

				return Res.of(tree);
			} catch (Exception e) {
				return Res.error("failed to build minimal spanning tree", e);
			}
		}
	}

}
