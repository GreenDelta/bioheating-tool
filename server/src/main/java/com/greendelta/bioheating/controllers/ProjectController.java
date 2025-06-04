package com.greendelta.bioheating.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greendelta.bioheating.services.ProjectService;
import com.greendelta.bioheating.services.ProjectService.ProjectData;
import com.greendelta.bioheating.services.ProjectService.ProjectInfo;
import com.greendelta.bioheating.services.UserService;
import com.greendelta.bioheating.util.Http;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

	private final ProjectService projects;
	private final UserService users;

	public ProjectController(ProjectService projects, UserService users) {
		this.projects = projects;
		this.users = users;
	}

	@GetMapping
	public ResponseEntity<?> getProjects(Authentication auth) {
		if (Http.isNotAuthenticated(auth)) {
			return Http.badRequest("not authenticated");
		}

		var projectList = projects.getProjectsForUser(auth.getName());
		var projectInfos = projectList.stream()
			.map(ProjectInfo::of)
			.toList();

		return Http.ok(projectInfos);
	}

	@GetMapping("/{id}")
	public ResponseEntity<?> getProject(
		Authentication auth, @PathVariable long id
	) {
		if (Http.isNotAuthenticated(auth)) {
			return Http.badRequest("not authenticated");
		}

		var project = projects.getProject(id, auth.getName()).orElse(null);
		return project == null
			? Http.notFound("project not found: " + id)
			: Http.ok(ProjectInfo.of(project));
	}

	@PostMapping
	public ResponseEntity<?> createProject(
		Authentication auth, @RequestBody ProjectData data
	) {
		if (Http.isNotAuthenticated(auth)) {
			return Http.badRequest("not authenticated");
		}

		var result = projects.createProject(data, auth.getName());
		return result.hasError()
			? Http.badRequest("failed to create project: " + result.error())
			: Http.ok(result.value());
	}
}
