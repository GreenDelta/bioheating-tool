package com.greendelta.bioheating.controllers;

import java.nio.file.Files;
import java.util.function.Function;

import org.openlca.commons.Res;
import org.openlca.commons.Strings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greendelta.bioheating.graph.Graph;
import com.greendelta.bioheating.graph.MinTreeSolution;
import com.greendelta.bioheating.graph.NetworkTree;
import com.greendelta.bioheating.graph.SteinerTree;
import com.greendelta.bioheating.io.SophenaExport;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.model.Solution;
import com.greendelta.bioheating.model.client.ClientSolution;
import com.greendelta.bioheating.pipes.PipeConfig;
import com.greendelta.bioheating.pipes.PipePlanXls;
import com.greendelta.bioheating.services.FileService;
import com.greendelta.bioheating.services.ProjectService;
import com.greendelta.bioheating.services.TaskService;
import com.greendelta.bioheating.services.TaskService.Task.NewTask;
import com.greendelta.bioheating.services.UserService;

@RestController
@RequestMapping("/api/solutions")
public class SolutionController {

	private final Database db;
	private final ProjectService projects;
	private final UserService users;
	private final TaskService tasks;
	private final FileService files;

	public SolutionController(
		Database db,
		ProjectService projects,
		UserService users,
		TaskService tasks,
		FileService files
	) {
		this.db = db;
		this.projects = projects;
		this.users = users;
		this.tasks = tasks;
		this.files = files;
	}

	@GetMapping("/{id}")
	public ResponseEntity<?> getSolution(
		Authentication auth,
		@PathVariable long id
	) {
		return withSolution(auth, id, solution ->
			Http.ok(ClientSolution.of(solution))
		);
	}

	@GetMapping("/{id}/image")
	public ResponseEntity<?> getSolutionImage(
		Authentication auth,
		@PathVariable long id
	) {
		return withSolution(auth, id, solution -> {
			var image = solution.image();
			if (
				image == null || image.length == 0
			) return ResponseEntity.notFound().build();
			return ResponseEntity.ok()
				.contentType(MediaType.IMAGE_PNG)
				.header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
				.body(image);
		});
	}

	@GetMapping("/{id}/xls")
	public ResponseEntity<?> getSolutionXls(
		Authentication auth,
		@PathVariable long id
	) {
		return withSolution(auth, id, solution -> {
			var treeRes = NetworkTree.of(solution);
			if (treeRes.isError()) return Http.serverError(treeRes.error());

			var config = PipeConfig.forPlastic().get();
			var xls = PipePlanXls.create(config, treeRes.value());
			if (xls.isError()) return Http.serverError(xls.error());

			var fileName = "pipe-plan-" + solution.id() + ".xlsx";
			return ResponseEntity.ok()
				.contentType(
					MediaType.parseMediaType(
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
					)
				)
				.header(
					HttpHeaders.CONTENT_DISPOSITION,
					"attachment; filename=\"" + fileName + "\""
				)
				.body(xls.value());
		});
	}

	@GetMapping("/{id}/sophena-package")
	public ResponseEntity<?> getSophenaPackage(
		Authentication auth,
		@PathVariable long id
	) {
		return withSolution(auth, id, solution -> {
			Res<byte[]> bytes = files.withTempFile(".gz", file -> {
				var res = SophenaExport.write(solution, file);
				if (res.isError()) return res.wrapError(
					"Failed to write Sophena package: " + res.error()
				);
				try {
					var bs = Files.readAllBytes(file.toPath());
					return Res.ok(bs);
				} catch (Exception e) {
					return Res.error("Failed to read exported Sophena package", e);
				}
			});
			if (bytes.isError()) return Http.serverError(bytes.error());

			var project = solution.project();
			var name =
				project != null && Strings.isNotBlank(project.name())
					? project.name().replaceAll("\\W+", "_")
					: "solution";
			return ResponseEntity.ok()
				.header(
					HttpHeaders.CONTENT_DISPOSITION,
					"attachment; filename=\"" + name + ".json.gz\""
				)
				.header(HttpHeaders.CONTENT_TYPE, "application/gzip")
				.body(bytes.value());
		});
	}

	@PostMapping("/project/{id}")
	public ResponseEntity<?> calculate(
		Authentication auth,
		@PathVariable long id
	) {
		return withProject(auth, id, project -> {
			var user = users.getCurrentUser(auth).orElse(null);
			if (user == null) return Http.badRequest("not authenticated");

			var task = NewTask.of(user, () -> {
				var graph = Graph.buildFrom(project);
				if (graph.isError()) return graph.wrapError(
					"failed to create project graph"
				);

				var tree = SteinerTree.compute(graph.value());
				if (tree.isError()) return tree.wrapError(
					"failed to create Steiner-Tree"
				);

				return new MinTreeSolution(project, tree.value()).create(db);
			});
			tasks.schedule(task);
			return Http.ok(task.toState());
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

	private ResponseEntity<?> withSolution(
		Authentication auth,
		long id,
		Function<Solution, ResponseEntity<?>> fn
	) {
		var user = users.getCurrentUser(auth).orElse(null);
		if (user == null) return Http.badRequest("not authenticated");
		var solution = db.getForId(Solution.class, id);
		if (solution == null) return Http.notFound("solution not found: " + id);
		var project = solution.project();
		return project == null || !user.equals(project.user())
			? Http.forbidden("access denied")
			: fn.apply(solution);
	}
}
