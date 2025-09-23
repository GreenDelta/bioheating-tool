package com.greendelta.bioheating.examples;

import java.util.concurrent.ThreadLocalRandom;

import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.alg.spanning.KruskalMinimumSpanningTree;

import com.greendelta.bioheating.Tests;
import com.greendelta.bioheating.calc.graph.Graph;
import com.greendelta.bioheating.calc.graph.Node.BuildingNode;
import com.greendelta.bioheating.model.Project;

public class GraphExample {

	public static void main(String[] args) {
		try (var db = Tests.db()) {
			System.out.println("Load project ...");
			var project = db.getAll(Project.class).getFirst();

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

		}
	}

}
