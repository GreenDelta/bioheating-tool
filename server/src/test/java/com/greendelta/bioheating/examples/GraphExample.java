package com.greendelta.bioheating.examples;

import com.greendelta.bioheating.Tests;
import com.greendelta.bioheating.calc.graph.Graph;
import com.greendelta.bioheating.model.Project;

public class GraphExample {

	public static void main(String[] args) {


		try (var db = Tests.db()) {
			System.out.println("Load project");
			var project = db.getAll(Project.class).getFirst();
			System.out.println("Build graph");

			var g = Graph.buildFrom(project);

			System.out.printf("  .. created graph with %d nodes and %d edges%n",
				g.vertexSet().size(), g.edgeSet().size());

		}
	}

}
