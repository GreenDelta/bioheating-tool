package com.greendelta.bioheating.examples;

import com.greendelta.bioheating.graph.Graph;
import com.greendelta.bioheating.graph.MinTreeSolution;
import com.greendelta.bioheating.graph.NetworkTree;
import com.greendelta.bioheating.graph.SteinerTree;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.pipes.PipeConfig;
import com.greendelta.bioheating.pipes.PipePlanXls;
import java.nio.file.Files;
import java.nio.file.Path;

public class PipePlanXlsExample {

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

			var tree = NetworkTree.of(solution.withTransientIds()).orElseThrow();
			var config = PipeConfig.forPlastic().get();

			var xlsRes = PipePlanXls.create(config, tree);
			if (xlsRes.isError()) {
				System.err.println("Error: " + xlsRes.error());
				return;
			}

			Files.write(Path.of("target/pipe-plan.xlsx"), xlsRes.value());
			System.out.println(
				"Pipe plan Excel file written to target/pipe-plan.xlsx"
			);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
