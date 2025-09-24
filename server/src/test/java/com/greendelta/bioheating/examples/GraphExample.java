package com.greendelta.bioheating.examples;

import java.awt.Color;
import java.io.File;
import java.util.concurrent.ThreadLocalRandom;

import javax.imageio.ImageIO;

import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.alg.spanning.KruskalMinimumSpanningTree;
import org.locationtech.jts.geom.Envelope;

import com.greendelta.bioheating.Tests;
import com.greendelta.bioheating.calc.graph.Graph;
import com.greendelta.bioheating.calc.graph.Node.BuildingNode;
import com.greendelta.bioheating.io.GeoImage;
import com.greendelta.bioheating.model.Project;

public class GraphExample {

	public static void main(String[] args) {
		try (var db = Tests.db()) {
			System.out.println("Load project ...");
			var project = db.getAll(Project.class).getFirst();

			// Calculate envelope for the image
			var envelope = new Envelope();
			for (var street : project.map().streets()) {
				for (var c : street.coordinates()) {
					envelope.expandToInclude(c);
				}
			}

			System.out.println("Build full graph ...");
			var g = Graph.buildFrom(project);
			System.out.printf("  .. created graph with %d nodes and %d edges%n",
				g.vertexSet().size(), g.edgeSet().size());

			System.out.println("Select center node ...");
			var buildingNodes = g.vertexSet()
				.stream()
				.filter(BuildingNode.class::isInstance)
				.map(BuildingNode.class::cast)
				.toList();
			var cix = ThreadLocalRandom
				.current()
				.nextInt(buildingNodes.size());
			var center = buildingNodes.get(cix);
			System.out.printf("  .. selected center: %d%n", center.id());

			System.out.println("Build minimal graph ...");
			var minGraph = new Graph();
			for (var target : buildingNodes) {
				if (target.equals(center))
					continue;
				var path = DijkstraShortestPath.findPathBetween(g, center, target);
				minGraph.add(path);
			}
			System.out.printf("  .. created graph with %d nodes and %d edges%n",
				minGraph.vertexSet().size(), minGraph.edgeSet().size());

			System.out.println("Build MST");
			var tree = new KruskalMinimumSpanningTree<>(minGraph)
				.getSpanningTree();
			System.out.printf("  .. created tree with %d edges%n",
				tree.getEdges().size());

			System.out.println("Drawing image ...");
			try (var img = new GeoImage(1024, 800, envelope)) {
				// Draw streets
				for (var street : project.map().streets()) {
					img.drawLine(street.coordinates(), Color.DARK_GRAY);
				}

				// Draw buildings
				var buildingColor = new Color(216, 27, 96);
				for (var building : project.map().buildings()) {
					img.drawPolygon(building.coordinates(), buildingColor);
				}

				// Draw minimal spanning tree edges in red
				for (var edge : tree.getEdges()) {
					img.draw(edge.line(), Color.RED);
				}

				ImageIO.write(img.getImage(), "png", new File("target/graph-with-mst.png"));
				System.out.println("  .. saved image to target/graph-with-mst.png");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
