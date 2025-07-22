package com.greendelta.bioheating.controllers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.function.Function;

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

import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.model.client.ClientProject;
import com.greendelta.bioheating.services.ProjectService;
import com.greendelta.bioheating.services.UserService;
import com.greendelta.bioheating.util.Http;
import com.greendelta.bioheating.util.Strings;

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
			.map(ProjectInfo::of)
			.toList();
		return Http.ok(data);
	}

	@GetMapping("/{id}")
	public ResponseEntity<?> getProject(
		Authentication auth, @PathVariable long id
	) {
		return withProject(auth, id, project -> {
			var res = ClientProject.of(project);
			return res.hasError()
				? Http.serverError("failed to convert project: " + res.error())
				: Http.ok(res.value());
		});
	}

	@PostMapping
	public ResponseEntity<?> createProject(
		Authentication auth,
		@RequestParam("name") String name,
		@RequestParam("climateRegion") int climateRegionId,
		@RequestParam("description") String description,
		@RequestParam("file") MultipartFile file
	) {

		// check input data
		var user = users.getUser(auth).orElse(null);
		if (user == null)
			return Http.badRequest("not authenticated");
		if (Strings.isNil(name))
			return Http.badRequest("a project name is required");
		if (file.isEmpty())
			return Http.badRequest("a CityGML file is required");



		try {

			// copy the uploaded file
			var uploadDir = Paths.get("uploads");
			if (!Files.exists(uploadDir)) {
				Files.createDirectories(uploadDir);
			}
			var path = uploadDir.resolve(UUID.randomUUID() + ".gml");
			try (var stream = file.getInputStream()) {
				Files.copy(stream, path);
			}

			// create the project
			var res = projects.createProject(
				user, name, description, path.toFile());
			if (res.hasError())
				return Http.serverError(res.error());
			var info = ProjectInfo.of(res.value());
			return Http.ok(info);

		} catch (IOException e) {
			return Http.serverError("project creation failed: " + e.getMessage());
		}
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteProject(
		Authentication auth, @PathVariable long id
	) {
		return withProject(auth, id, project -> {
			var err = projects.delete(project);
			return err.hasError()
				? Http.badRequest("failed to delete project: " + err.error())
				: Http.ok("project deleted successfully");
		});
	}

	@PostMapping("/{id}")
	public ResponseEntity<?> updateProject(
		Authentication auth, @PathVariable long id, @RequestBody ClientProject data
	) {
		return withProject(auth, id, project -> {
			data.writeUpdatesTo(project);
			var res = projects.updateProject(project);
			return res.hasError()
				? Http.serverError("failed to save project: " + res.error())
				: Http.ok(ProjectInfo.of(project));
		});
	}

	private ResponseEntity<?> withProject(
		Authentication auth, long id, Function<Project, ResponseEntity<?>> fn
	) {
		var user = users.getUser(auth).orElse(null);
		if (user == null)
			return Http.badRequest("not authenticated");
		var project = projects.getProject(user, id).orElse(null);
		return project == null
			? Http.notFound("project not found: " + id)
			: fn.apply(project);
	}

	public record ProjectInfo(
		long id, String name, String description
	) {

		public static ProjectInfo of(Project p) {
			return new ProjectInfo(
				p.id(), p.name(), p.description()
			);
		}
	}

}
