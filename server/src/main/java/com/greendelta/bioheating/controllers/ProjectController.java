package com.greendelta.bioheating.controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.greendelta.bioheating.io.CityGmlImport;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.services.ProjectService;
import com.greendelta.bioheating.services.ProjectService.ProjectData;
import com.greendelta.bioheating.services.UserService;
import com.greendelta.bioheating.util.Http;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

	private final Database db;
	private final ProjectService projects;
	private final UserService users;

	public ProjectController(
		Database db, ProjectService projects, UserService users
	) {
		this.db = db;
		this.projects = projects;
		this.users = users;
	}

	@GetMapping
	public ResponseEntity<?> getProjects(Authentication auth) {
		var user = users.getUser(auth).orElse(null);
		if (user == null)
			return Http.badRequest("not authenticated");
		var data = projects.getProjects(user).stream()
			.map(ProjectData::of)
			.toList();
		return Http.ok(data);
	}

	@GetMapping("/{id}")
	public ResponseEntity<?> getProject(
		Authentication auth, @PathVariable long id
	) {
		var user = users.getUser(auth).orElse(null);
		if (user == null)
			return Http.badRequest("not authenticated");
		var project = projects.getProject(user, id).orElse(null);
		return project == null
			? Http.notFound("project not found: " + id)
			: Http.ok(ProjectData.of(project));
	}

	@PostMapping
	public ResponseEntity<?> createProject(
		Authentication auth, @RequestBody ProjectData data
	) {
		var user = users.getUser(auth).orElse(null);
		if (user == null)
			return Http.badRequest("not authenticated");

		var result = projects.createProject(user, data);
		return result.hasError()
			? Http.badRequest("failed to create project: " + result.error())
			: Http.ok(result.value());
	}

	@PostMapping("/with-file")
	public ResponseEntity<?> createProjectWithFile(
		Authentication auth,
		@RequestParam("name") String name,
		@RequestParam("description") String description,
		@RequestParam("cityGmlFile") MultipartFile cityGmlFile
	) {
		var user = users.getUser(auth).orElse(null);
		if (user == null)
			return Http.badRequest("not authenticated");

		// Validate file
		if (cityGmlFile.isEmpty())
			return Http.badRequest("CityGML file is required");

		// Check file extension
		String originalFilename = cityGmlFile.getOriginalFilename();
		if (originalFilename == null || (!originalFilename.toLowerCase().endsWith(".gml") && !originalFilename.toLowerCase().endsWith(".xml")))
			return Http.badRequest("Only .gml and .xml files are allowed");

		try {
			// Create uploads directory if it doesn't exist
			Path uploadDir = Paths.get("uploads");
			if (!Files.exists(uploadDir)) {
				Files.createDirectories(uploadDir);
			}

			// Generate unique filename
			String fileExtension = originalFilename.substring(originalFilename.lastIndexOf('.'));
			String uniqueFileName = UUID.randomUUID() + fileExtension;
			Path filePath = uploadDir.resolve(uniqueFileName);

			// Save file temporarily
			Files.copy(cityGmlFile.getInputStream(), filePath);

			// Create project without file first
			var projectData = new ProjectData(0, name, description);
			var createResult = projects.createProject(user, projectData);

			if (createResult.hasError()) {
				Files.deleteIfExists(filePath); // Clean up file
				return Http.badRequest("failed to create project: " + createResult.error());
			}

			// run the import
			File file = filePath.toFile();
			var res = new CityGmlImport(db, createResult.value(), file).call();
			Files.deleteIfExists(filePath);
			return res.hasError()
				? Http.badRequest("failed to import CityGML: " + res.error())
				: Http.ok(ProjectData.of(res.value()));

		} catch (IOException e) {
			return Http.badRequest("failed to save file: " + e.getMessage());
		}
	}

	@PostMapping("/{id}/import-citygml")
	public ResponseEntity<?> importCityGml(
		Authentication auth,
		@PathVariable long id,
		@RequestParam("cityGmlFile") MultipartFile cityGmlFile
	) {
		var user = users.getUser(auth).orElse(null);
		if (user == null)
			return Http.badRequest("not authenticated");

		// Get the existing project
		var project = projects.getProject(user, id).orElse(null);
		if (project == null)
			return Http.notFound("project not found: " + id);

		// Validate file
		if (cityGmlFile.isEmpty())
			return Http.badRequest("CityGML file is required");

		// Check file extension
		String originalFilename = cityGmlFile.getOriginalFilename();
		if (originalFilename == null || (!originalFilename.toLowerCase().endsWith(".gml") && !originalFilename.toLowerCase().endsWith(".xml")))
			return Http.badRequest("Only .gml and .xml files are allowed");

		try {
			// Create uploads directory if it doesn't exist
			Path uploadDir = Paths.get("uploads");
			if (!Files.exists(uploadDir)) {
				Files.createDirectories(uploadDir);
			}

			// Generate unique filename
			String fileExtension = originalFilename.substring(originalFilename.lastIndexOf('.'));
			String uniqueFileName = UUID.randomUUID() + fileExtension;
			Path filePath = uploadDir.resolve(uniqueFileName);

			// Save file temporarily
			Files.copy(cityGmlFile.getInputStream(), filePath);

			// Import buildings from CityGML
			File file = filePath.toFile();
			var res = new CityGmlImport(db, project, file).call();
			Files.deleteIfExists(filePath);
			return res.hasError()
				? Http.badRequest("failed to import CityGML: " + res.error())
				: Http.ok(ProjectData.of(res.value()));

		} catch (IOException e) {
			return Http.badRequest("failed to process file: " + e.getMessage());
		}
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteProject(
		Authentication auth, @PathVariable long id
	) {
		var user = users.getUser(auth).orElse(null);
		if (user == null)
			return Http.badRequest("not authenticated");

		var result = projects.deleteProject(user, id);
		return result.hasError()
			? Http.badRequest("failed to delete project: " + result.error())
			: Http.ok("project deleted successfully");
	}
}
