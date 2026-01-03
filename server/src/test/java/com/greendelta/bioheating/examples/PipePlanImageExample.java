package com.greendelta.bioheating.examples;

import java.nio.file.Files;
import java.nio.file.Path;

import com.greendelta.bioheating.graph.Graph;
import com.greendelta.bioheating.graph.MinTreeSolution;
import com.greendelta.bioheating.graph.SteinerTree;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.pipes.PipePlanImage;

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
				.then(steiner -> new MinTreeSolution(project, steiner).create())
				.orElseThrow();

			var imageRes = PipePlanImage.create(solution);
			if (imageRes.isError()) {
				System.err.println("Error: " + imageRes.error());
				return;
			}

			Files.write(Path.of("target/pipe-plan.png"), imageRes.value());
			System.out.println("Pipe plan image written to target/pipe-plan.png");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
