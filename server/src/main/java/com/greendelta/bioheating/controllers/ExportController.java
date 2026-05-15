package com.greendelta.bioheating.controllers;

import com.greendelta.bioheating.io.XlsBuildingExport;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.services.ProjectService;
import com.greendelta.bioheating.services.UserService;
import org.openlca.commons.Strings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

@RestController
@RequestMapping("/api/export")
public class ExportController {

	private final ProjectService projects;
	private final UserService users;

	public ExportController(ProjectService projects, UserService users) {
		this.projects = projects;
		this.users = users;
	}

	@PostMapping("/buildings-xls/{projectId}")
	public ResponseEntity<?> projectBuildingsToExcel(
		Authentication auth,
		@PathVariable long projectId,
		@RequestBody(required = false) List<Long> ids
	) {
		return withProject(auth, projectId, project -> {
			var selectedIds = ids == null ? null : Set.copyOf(ids);
			try (var out = new ByteArrayOutputStream()) {
				var export = new XlsBuildingExport(project, out)
					.withIds(selectedIds)
					.run();
				if (export.isError()) return Http.serverError(export.error());

				var fileName = fileNameOf(project);
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
					.body(out.toByteArray());
			} catch (Exception e) {
				return Http.serverError("failed to export buildings: " + e.getMessage());
			}
		});
	}

	private String fileNameOf(Project project) {
		if (project == null || Strings.isBlank(project.name())) {
			return "buildings.xlsx";
		}
		return project.name().replaceAll("\\W+", "_") + "_buildings.xlsx";
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
			? Http.notFound("project not found: " + id)
			: fn.apply(project);
	}

}
