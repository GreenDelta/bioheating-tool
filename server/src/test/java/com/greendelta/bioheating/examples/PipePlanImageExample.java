package com.greendelta.bioheating.examples;

import com.greendelta.bioheating.graph.Graph;
import com.greendelta.bioheating.graph.MinTreeSolution;
import com.greendelta.bioheating.graph.SteinerTree;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Project;
import java.nio.file.Files;
import java.nio.file.Path;

public class PipePlanImageExample {

	public static void main(String[] args) {
		var db = Database.of("bioheating")
			.withUser("postgres", "bioheating")
			.withHost("localhost", 5432)
			.connect();

		try (db) {
			var project = db.getAll(Project.class).getFirst();
			System.out.println("Loaded project: " + project);

			var solution = Graph.buildFrom(project)
				.then(SteinerTree::compute)
				.then(steiner -> new MinTreeSolution(project, steiner).create(db))
				.orElseThrow();

			Files.write(Path.of("target/pipe-plan.png"), solution.image());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
