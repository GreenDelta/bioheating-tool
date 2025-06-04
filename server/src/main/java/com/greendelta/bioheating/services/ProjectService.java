package com.greendelta.bioheating.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.model.Res;
import com.greendelta.bioheating.model.User;
import com.greendelta.bioheating.util.Strings;

@Service
public class ProjectService {

	private final Database db;

	public ProjectService(Database db) {
		this.db = db;
	}

	public List<Project> getProjectsForUser(String username) {
		var user = db.get(User.class, "name", username).orElse(null);
		if (user == null) {
			return List.of();
		}
		return db.getAll(
			"select p from Project p where p.user.name = ?1 order by p.name",
			Project.class, username
		);
	}

	public Optional<Project> getProject(long projectId, String username) {
		var project = db.get(Project.class, projectId).orElse(null);
		if (project == null) {
			return Optional.empty();
		}
		// Check if the project belongs to the user
		if (project.user() == null || !Strings.eq(project.user().name(), username)) {
			return Optional.empty();
		}
		return Optional.of(project);
	}

	public Res<ProjectInfo> createProject(ProjectData data, String username) {
		if (data == null) {
			return Res.error("no project data given");
		}
		if (Strings.nullOrEmpty(data.name)) {
			return Res.error("project name is required");
		}

		var user = db.get(User.class, "name", username).orElse(null);
		if (user == null) {
			return Res.error("user not found: " + username);
		}

		// Check if project name already exists for this user
		var existing = db.getFirst(
			"select p from Project p where p.user.name = ?1 and p.name = ?2",
			Project.class, username, data.name
		);
		if (existing.isPresent()) {
			return Res.error("project with name '" + data.name + "' already exists");
		}

		var project = new Project()
			.name(data.name)
			.description(data.description)
			.user(user);

		db.insert(project);
		return Res.of(ProjectInfo.of(project));
	}

	public static class ProjectData {
		public String name;
		public String description;
	}

	public static class ProjectInfo {
		public long id;
		public String name;
		public String description;

		public static ProjectInfo of(Project project) {
			var info = new ProjectInfo();
			info.id = project.id();
			info.name = project.name();
			info.description = project.description();
			return info;
		}
	}
}
