package com.greendelta.bioheating.controllers;

import java.util.function.Function;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greendelta.bioheating.calc.graph.Graph;
import com.greendelta.bioheating.calc.graph.MinTreeSolution;
import com.greendelta.bioheating.calc.graph.SteinerTree;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.model.Solution;
import com.greendelta.bioheating.model.client.ClientSolution;
import com.greendelta.bioheating.services.ProjectService;
import com.greendelta.bioheating.services.TaskService;
import com.greendelta.bioheating.services.TaskService.Task.NewTask;
import com.greendelta.bioheating.services.UserService;
import com.greendelta.bioheating.util.Http;

@RestController
@RequestMapping("/api/solutions")
public class SolutionController {

	private final Database db;
	private final ProjectService projects;
	private final UserService users;
	private final TaskService tasks;

	public SolutionController(
		Database db,
		ProjectService projects,
		UserService users,
		TaskService tasks
	) {
		this.db = db;
		this.projects = projects;
		this.users = users;
		this.tasks = tasks;
	}

	@GetMapping("/{id}")
	public ResponseEntity<?> getSolution(
		Authentication auth, @PathVariable long id
	) {
		return withSolution(
			auth, id, solution -> Http.ok(ClientSolution.of(solution)));
	}

	@GetMapping("/{id}/image")
	public ResponseEntity<?> getSolutionImage(
		Authentication auth, @PathVariable long id
	) {
		return withSolution(auth, id, solution -> {
			var image = solution.image();
			if (image == null || image.length == 0)
				return ResponseEntity.notFound().build();
			return ResponseEntity.ok()
				.contentType(MediaType.IMAGE_PNG)
				.header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
				.body(image);
		});
	}

	@PostMapping("/project/{id}")
	public ResponseEntity<?> calculate(
		Authentication auth, @PathVariable long id
	) {
		return withProject(auth, id, project -> {
			var user = users.getCurrentUser(auth).orElse(null);
			if (user == null)
				return Http.badRequest("not authenticated");

			var task = NewTask.of(user, () -> {

				var graph = Graph.buildFrom(project);
				if (graph.hasError())
					return graph.wrapError("failed to create project graph");

				var tree = SteinerTree.compute(graph.value());
				if (tree.hasError())
					return tree.wrapError("failed to create Steiner-Tree");

				var res = new MinTreeSolution(project, tree.value()).create();
				if (res.hasError())
					return res;

				var solution = res.value();
				solution.calculatedAt(System.currentTimeMillis());
				db.insert(solution);
				deleteOutdatedOf(solution);
				return res;
			});
			tasks.schedule(task);
			return Http.ok(task.toState());
		});
	}

	private void deleteOutdatedOf(Solution solution) {
		if (solution == null || solution.project() == null)
			return;
		for (var s : db.getAll(Solution.class)) {
			if (solution.equals(s)
				|| !solution.project().equals(s.project()))
				continue;
			db.delete(s);
		}
	}

	private ResponseEntity<?> withProject(
		Authentication auth, long id, Function<Project, ResponseEntity<?>> fn
	) {
		var user = users.getCurrentUser(auth).orElse(null);
		if (user == null)
			return Http.badRequest("not authenticated");
		var project = projects.getProject(user, id).orElse(null);
		return project == null
			? Http.notFound("project not found: " + id)
			: fn.apply(project);
	}

	private ResponseEntity<?> withSolution(
		Authentication auth, long id, Function<Solution, ResponseEntity<?>> fn
	) {
		var user = users.getCurrentUser(auth).orElse(null);
		if (user == null)
			return Http.badRequest("not authenticated");
		var solution = db.getForId(Solution.class, id);
		if (solution == null)
			return Http.notFound("solution not found: " + id);
		var project = solution.project();
		return project == null || !user.equals(project.user())
			? Http.forbidden("access denied")
			: fn.apply(solution);
	}
}
