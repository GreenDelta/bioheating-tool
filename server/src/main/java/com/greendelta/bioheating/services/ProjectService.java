package com.greendelta.bioheating.services;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.model.User;
import com.greendelta.bioheating.util.Res;
import com.greendelta.bioheating.util.Strings;

@Service
public class ProjectService {

	private final Database db;

	public ProjectService(Database db) {
		this.db = db;
	}

	public List<Project> getProjects(User user) {
		if (user == null)
			return List.of();
		return db.getAll(Project.class).stream()
			.filter(p -> Objects.equals(p.user(), user))
			.toList();
	}

	public Optional<Project> getProject(User user, long id) {
		if (user == null)
			return Optional.empty();
		var p = db.getForId(Project.class, id);
		return p != null && Objects.equals(user, p.user())
			? Optional.of(p)
			: Optional.empty();
	}
	public Res<Project> createProject(User user, ProjectData data) {
		if (user == null || data == null)
			return Res.error("no user or project data given");
		if (Strings.isNil(data.name))
			return Res.error("project name is required");

		var project = new Project()
			.name(data.name)
			.description(data.description)
			.user(user);
		db.insert(project);
		return Res.of(project);
	}

	public Res<Void> deleteProject(User user, long projectId) {
		if (user == null)
			return Res.error("no user given");
		var project = db.getForId(Project.class, projectId);
		if (project == null)
			return Res.error("project not found");
		if (!Objects.equals(user, project.user()))
			return Res.error("not authorized to delete this project");
		db.delete(project);
		return Res.VOID;
	}
	public record ProjectData(
		long id, String name, String description
	) {

		public static ProjectData of(Project p) {
			return new ProjectData(
				p.id(), p.name(), p.description()
			);
		}
	}

}
