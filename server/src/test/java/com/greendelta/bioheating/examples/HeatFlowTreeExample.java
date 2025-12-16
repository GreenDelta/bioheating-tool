package com.greendelta.bioheating.examples;

import java.util.Comparator;

import org.openlca.commons.Res;

import com.greendelta.bioheating.graph.Graph;
import com.greendelta.bioheating.graph.HeatFlowTree;
import com.greendelta.bioheating.graph.MinTreeSolution;
import com.greendelta.bioheating.graph.SteinerTree;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.model.Solution;

public class HeatFlowTreeExample {

	public static void main(String[] args) {
		var db = Database.of("bioheating")
			.withUser("postgres", "bioheating")
			.withHost("localhost", 5432)
			.connect();

		try (db) {
			var project = db.getAll(Project.class).stream()
				.min(Comparator.comparingLong(Project::id))
				.orElse(null);

			if (project == null) {
				System.out.println("No projects found in database");
				return;
			}

			System.out.println("Loaded project: " + project);

			var res = calculate(project);
			if (res.isError()) {
				System.err.println("Solution calculation failed: " + res.error());
				return;
			}

			var solution = res.value();
			System.out.println("Created solution for project " + project.name());

			var calcTree = HeatFlowTree.of(solution);
			if (calcTree.isError()) {
				System.err.println("CalcTree build failed: " + calcTree.error());
				return;
			}
			System.out.println(calcTree.value().toDot());
		}
	}

	private static Res<Solution> calculate(Project project) {
		var graph = Graph.buildFrom(project);
		if (graph.isError())
			return graph.wrapError("failed to create project graph");

		var tree = SteinerTree.compute(graph.value());
		if (tree.isError())
			return tree.wrapError("failed to create Steiner-Tree");

		var res = new MinTreeSolution(project, tree.value()).create();
		if (res.isError())
			return res;

		var solution = res.value();
		solution.calculatedAt(System.currentTimeMillis());
		return Res.ok(solution);
	}

}
