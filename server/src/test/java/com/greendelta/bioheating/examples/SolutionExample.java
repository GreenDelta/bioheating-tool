package com.greendelta.bioheating.examples;

import com.greendelta.bioheating.Tests;
import com.greendelta.bioheating.calc.Solution;
import com.greendelta.bioheating.model.Project;

public class SolutionExample {

	public static void main(String[] args) {

		try (var db = Tests.db()) {
			System.out.println("Fetch project");
			var project = db.getAll(Project.class).getFirst();
			System.out.println("Calculate solution");
			var solution = Solution.calculate(project.map());
			System.out.printf(
				"Solution with %d buildings, %d streets, and %d connectors%n",
				solution.buildings().size(),
				solution.streets().size(),
				solution.connectors().size());
		}
	}
}
