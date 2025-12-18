package com.greendelta.bioheating.examples;

import org.openlca.commons.Res;

import com.greendelta.bioheating.graph.Graph;
import com.greendelta.bioheating.graph.HeatFlowTree;
import com.greendelta.bioheating.graph.HeatFlowViz;
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
				.then(solution -> Res.ok(solution.withTransientIds()))
				.then(HeatFlowTree::of)
				.orElseThrow();

			render(HeatFlowViz.toDot(tree), "target/tree.png");
			render(HeatFlowViz.toDot(tree.compact()), "target/tree_compact.png");

		}
	}

	private static void render(String dot, String file) {
		try {
			var p = new ProcessBuilder("dot", "-Tpng", "-o", file)
				.start();
			try (var os = p.getOutputStream()) {
				os.write(dot.getBytes());
			}
			p.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
