package com.greendelta.bioheating.examples;

import com.greendelta.bioheating.graph.Graph;
import com.greendelta.bioheating.graph.HeatFlowTree;
import com.greendelta.bioheating.graph.MinTreeSolution;
import com.greendelta.bioheating.graph.SteinerTree;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Project;

public class HeatFlowTreeExample {

	public static void main(String[] args) {
		var db = Database.of("bioheating")
			.withUser("postgres", "bioheating")
			.withHost("localhost", 5432)
			.connect();

		try (db) {
			var project = db.getAll(Project.class).getFirst();
			System.out.println("Loaded project: " + project);
			var tree = Graph.buildFrom(project)
				.then(SteinerTree::compute)
				.then(steiner -> new MinTreeSolution(project, steiner).create())
				.then(HeatFlowTree::of)
				.orElseThrow();
			System.out.println(tree.toDot());
		}
	}

}
