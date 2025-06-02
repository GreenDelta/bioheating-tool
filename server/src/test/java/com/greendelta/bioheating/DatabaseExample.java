package com.greendelta.bioheating;

import com.greendelta.bioheating.model.Database;

public class DatabaseExample {

	public static void main(String[] args) {
		var db = Database.of("bioheating")
			.withUser("postgres", "therm0s")
			.withHost("localhost", 5432)
			.connect();
		try (db) {
			System.out.println("Database connected: " + db);
		}

	}
}
