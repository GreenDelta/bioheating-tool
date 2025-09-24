package com.greendelta.bioheating.examples;

import java.nio.file.Files;
import java.nio.file.Paths;

import com.greendelta.bioheating.Tests;
import com.greendelta.bioheating.calc.graph.MinTree;
import com.greendelta.bioheating.model.Project;

public class MinTreeExample {

	public static void main(String[] args) {
		try (var db = Tests.db()) {
			var project = db.getAll(Project.class).getFirst();
			var solution = MinTree.solutionOf(project).orElseThrow();
			Files.write(Paths.get("target", "solution.png"), solution.image());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
