package com.greendelta.bioheating.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greendelta.bioheating.model.ClimateRegion;
import com.greendelta.bioheating.model.Database;

@RestController
@RequestMapping("/api/climate-regions")
public class ClimateRegionController {

	private final Database db;

	public ClimateRegionController(Database db) {
		this.db = db;
	}

	@GetMapping
	public ResponseEntity<?> getClimateRegions() {
		var regions = db.getAll(ClimateRegion.class);
		return ResponseEntity.ok(regions);
	}
}
