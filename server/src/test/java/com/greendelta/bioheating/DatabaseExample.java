package com.greendelta.bioheating;

import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Project;

public class DatabaseExample {

	public static void main(String[] args) {
		var db = Database.of("bioheating")
			.withUser("postgres", "therm0s")
			.withHost("localhost", 5432)
			.connect();
		try (db) {
			var project = new Project()
				.name("Project 1")
				.description("This is a test project");
			db.insert(project);
			System.out.println("Database connected: " + db);
		}

	}
}
