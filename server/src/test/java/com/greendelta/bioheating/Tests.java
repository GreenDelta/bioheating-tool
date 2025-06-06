package com.greendelta.bioheating;

import com.greendelta.bioheating.model.Database;

public class Tests {

	private static final Database db = Database.of("bioheating")
		.withUser("postgres", "bioheating")
		.connect();

	public static Database db() {
		return db;
	}
}
