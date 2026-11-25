package com.greendelta.bioheating.controllers;

import com.greendelta.bioheating.io.ImportFileType;
import com.greendelta.bioheating.io.ProjectCreator;
import com.greendelta.bioheating.io.ProjectUpdate;
import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.model.client.ClientProject;
import com.greendelta.bioheating.model.client.GeoFeature;
import com.greendelta.bioheating.services.FileService;
import com.greendelta.bioheating.services.ModelService;
import com.greendelta.bioheating.services.ProjectService;
import com.greendelta.bioheating.services.TaskService;
import com.greendelta.bioheating.services.TaskService.Task.NewTask;
import com.greendelta.bioheating.services.TaskService.TaskType;
import com.greendelta.bioheating.services.UserService;
import org.springframework.http.ResponseEntity;
import com.greendelta.bioheating.services.ErrorCode;
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

import org.openlca.commons.Res;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

	private final Database db;
	private final ProjectService projects;
	private final UserService users;
	private final FileService files;
	private final TaskService tasks;
	private final ModelService model;

	public ProjectController(
		Database db,
		ProjectService projects,
		UserService users,
		FileService files,
		TaskService tasks,
		ModelService model
	) {
		this.db = db;
		this.projects = projects;
		this.users = users;
		this.files = files;
		this.tasks = tasks;
		this.model = model;
	}

	@GetMapping
	public ResponseEntity<?> getProjects(Authentication auth) {
		var user = users.getCurrentUser(auth).orElse(null);
		if (user == null) return Http.unauthorized();
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
				? Http.serverError(ErrorCode.PROJECT_LOAD_FAILED, res.error())
				: Http.ok(res.value());
		});
	}

	@PostMapping
	public ResponseEntity<?> createProject(
		Authentication auth,
		@RequestParam("name") String name,
		@RequestParam(value = "description", required = false) String description,
		@RequestParam("file") MultipartFile[] uploads
	) {

		var user = users.getCurrentUser(auth).orElse(null);
		if (user == null) {
			return Http.unauthorized();
		}

		// upload geo-data
		var geoFiles = files.saveUploads(uploads);
		if (geoFiles.isError()) {
			return Http.badRequest(ErrorCode.UPLOAD_FAILED, geoFiles.error());
		}
		if (geoFiles.value().isEmpty()) {
			return Http.badRequest(ErrorCode.NO_VALID_FILES);
		}

		// create the project and start the import task
		var project = new Project()
			.name(name)
			.description(description)
			.user(user);
		var task = NewTask.of(user, TaskType.PROJECT_IMPORT, () ->
			files.useFiles(geoFiles.value(),
				gmlFiles -> new ProjectCreator(db, project, gmlFiles, model.predictor()).call())
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
				? Http.badRequest(ErrorCode.PROJECT_DELETE_FAILED, err.error())
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
				? Http.serverError(ErrorCode.PROJECT_SAVE_FAILED, res.error())
				: Http.ok(ProjectInfo.of(project));
		});
	}

	/// Updates the data of a project in a background task, see `ProjectUpdate`.
	/// The update steps are optional but at least one has to be selected.
	@PostMapping("/{id}/data-update")
	public ResponseEntity<?> updateProjectData(
		Authentication auth,
		@PathVariable long id,
		@RequestParam(value = "file", required = false) MultipartFile upload,
		@RequestParam(value = "updateStreets", defaultValue = "false")
		boolean updateStreets,
		@RequestParam(value = "estimateDemand", defaultValue = "false")
		boolean estimateDemand
	) {
		var user = users.getCurrentUser(auth).orElse(null);
		if (user == null) return Http.unauthorized();
		var project = projects.getProject(user, id).orElse(null);
		if (project == null)
			return Http.notFound(ErrorCode.PROJECT_NOT_FOUND, "project not found: " + id);

		boolean hasFile = upload != null && !upload.isEmpty();
		if (!hasFile && !updateStreets && !estimateDemand)
			return Http.badRequest(ErrorCode.NO_UPDATE_SELECTED);
		if (hasFile && ImportFileType.ofName(upload.getOriginalFilename())
				!= ImportFileType.EXCEL)
			return Http.badRequest(ErrorCode.NO_VALID_FILES);

		var update = new ProjectUpdate(db, project, model.predictor())
			.withStreets(updateStreets)
			.withEstimation(estimateDemand);

		NewTask<ProjectInfo> task;
		if (hasFile) {
			// the upload must be saved within the request thread
			var saved = files.saveUploads(new MultipartFile[]{upload});
			if (saved.isError())
				return Http.badRequest(ErrorCode.UPLOAD_FAILED, saved.error());
			if (saved.value().size() != 1)
				return Http.badRequest(ErrorCode.NO_VALID_FILES);
			task = NewTask.of(user, TaskType.PROJECT_UPDATE, () ->
				files.useFiles(saved.value(), excel -> update
					.withExcelFile(excel.getFirst())
					.call()
					.then(p -> Res.ok(ProjectInfo.of(p)))));
		} else {
			task = NewTask.of(user, TaskType.PROJECT_UPDATE, () ->
				update.call().then(p -> Res.ok(ProjectInfo.of(p))));
		}
		tasks.schedule(task);
		return Http.ok(task.toState());
	}

	@PostMapping("/{id}/estimate-building")
	public ResponseEntity<?> estimateBuilding(
		Authentication auth,
		@PathVariable long id,
		@RequestBody GeoFeature data
	) {
		return withProject(auth, id, project -> {
			if (data == null || !data.isBuilding()) {
				return Http.badRequest(ErrorCode.INVALID_BUILDING_DATA);
			}
			if (project.climateRegion() == null) {
				return Http.badRequest(ErrorCode.NO_CLIMATE_REGION);
			}

			var building = new Building();
			data.applyOn(building);
			if (!building.isHeated()) {
				return Http.badRequest(ErrorCode.BUILDING_NOT_HEATED);
			}

			var result = model.predictor().predict(project.climateRegion(), building);
			return result.isError()
				? Http.serverError(ErrorCode.ESTIMATION_FAILED, result.error())
				: Http.ok(result.value());
		});
	}

	private ResponseEntity<?> withProject(
		Authentication auth,
		long id,
		Function<Project, ResponseEntity<?>> fn
	) {
		var user = users.getCurrentUser(auth).orElse(null);
		if (user == null) return Http.unauthorized();
		var project = projects.getProject(user, id).orElse(null);
		return project == null
			? Http.notFound(ErrorCode.PROJECT_NOT_FOUND, "project not found: " + id)
			: fn.apply(project);
	}

	public record ProjectInfo(long id, String name, String description) {
		public static ProjectInfo of(Project p) {
			return new ProjectInfo(p.id(), p.name(), p.description());
		}
	}
}
