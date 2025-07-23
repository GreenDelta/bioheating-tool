package com.greendelta.bioheating.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Fuel;

@RestController
@RequestMapping("/api/fuels")
public class FuelController {

	private final Database db;

	public FuelController(Database db) {
		this.db = db;
	}

	@GetMapping
	public ResponseEntity<?> getFuels() {
		var fuels = db.getAll(Fuel.class);
		return ResponseEntity.ok(fuels);
	}
}
