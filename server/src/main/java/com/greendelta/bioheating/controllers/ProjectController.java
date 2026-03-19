package com.greendelta.bioheating.controllers;

import com.greendelta.bioheating.model.ClimateRegion;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.model.client.ClientProject;
import com.greendelta.bioheating.services.FileService;
import com.greendelta.bioheating.services.ProjectService;
import com.greendelta.bioheating.services.TaskService;
import com.greendelta.bioheating.services.TaskService.Task.NewTask;
import com.greendelta.bioheating.services.UserService;
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

import java.util.function.Function;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

	private final Database db;
	private final ProjectService projects;
	private final UserService users;
	private final FileService files;
	private final TaskService tasks;

	public ProjectController(
		Database db,
		ProjectService projects,
		UserService users,
		FileService files,
		TaskService tasks
	) {
		this.db = db;
		this.projects = projects;
		this.users = users;
		this.files = files;
		this.tasks = tasks;
	}

	@GetMapping
	public ResponseEntity<?> getProjects(Authentication auth) {
		var user = users.getCurrentUser(auth).orElse(null);
		if (user == null) return Http.badRequest("not authenticated");
		var data = projects
			.getProjects(user)
			.stream()
			.map(ProjectInfo::of)
			.toList();
		return Http.ok(data);
	}

	@GetMapping("/{id}")
	public ResponseEntity<?> getProject(
		Authentication auth,
		@PathVariable long id
	) {
		return withProject(auth, id, project -> {
			var res = ClientProject.of(db, project);
			return res.isError()
				? Http.serverError("failed to convert project: " + res.error())
				: Http.ok(res.value());
		});
	}

	@PostMapping
	public ResponseEntity<?> createProject(
		Authentication auth,
		@RequestParam("name") String name,
		@RequestParam("climateRegionId") int climateRegionId,
		@RequestParam(value = "description", required = false) String description,
		@RequestParam("file") MultipartFile[] uploads
	) {

		var user = users.getCurrentUser(auth).orElse(null);
		if (user == null) {
			return Http.badRequest("Not authenticated");
		}
		var region = db.getForId(ClimateRegion.class, climateRegionId);
		if (region == null) {
			return Http.badRequest(
				"No climate region found for ID=" + climateRegionId);
		}

		// upload geo-data
		var geoFiles = files.saveUploads(uploads);
		if (geoFiles.isError()) {
			return Http.badRequest(
				"Failed to save uploaded files: " + geoFiles.error());
		}
		if (geoFiles.value().isEmpty()) {
			return Http.badRequest(	"No valid files provided");
		}

		// create the project and start the import task
		var project = new Project()
			.name(name)
			.description(description)
			.climateRegion(region)
			.user(user);
		var task = NewTask.of(user, () ->
			files.useFiles(geoFiles.value(),
				gmlFiles -> projects.addMap(project, gmlFiles))
		);
		tasks.schedule(task);
		return Http.ok(task.toState());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteProject(
		Authentication auth,
		@PathVariable long id
	) {
		return withProject(auth, id, project -> {
			var err = projects.delete(project);
			return err.isError()
				? Http.badRequest("failed to delete project: " + err.error())
				: Http.ok("project deleted successfully");
		});
	}

	@PostMapping("/{id}")
	public ResponseEntity<?> updateProject(
		Authentication auth,
		@PathVariable long id,
		@RequestBody ClientProject data
	) {
		return withProject(auth, id, project -> {
			data.writeUpdatesTo(project);
			var res = projects.updateProject(project);
			return res.isError()
				? Http.serverError("failed to save project: " + res.error())
				: Http.ok(ProjectInfo.of(project));
		});
	}

	private ResponseEntity<?> withProject(
		Authentication auth,
		long id,
		Function<Project, ResponseEntity<?>> fn
	) {
		var user = users.getCurrentUser(auth).orElse(null);
		if (user == null) return Http.badRequest("not authenticated");
		var project = projects.getProject(user, id).orElse(null);
		return project == null
			? Http.notFound("project not found: " + id)
			: fn.apply(project);
	}

	public record ProjectInfo(long id, String name, String description) {
		public static ProjectInfo of(Project p) {
			return new ProjectInfo(p.id(), p.name(), p.description());
		}
	}
}
