package com.greendelta.bioheating.services;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.greendelta.bioheating.io.CityGmlImport;
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

	public Res<Project> createProject(
		User user, String name, String description, File gml
	) {
		if (user == null)
			return Res.error("a user is required");
		if (Strings.isNil(name))
			return Res.error("a project name is required");
		if (gml == null || !gml.exists())
			return Res.error("a CityGML file is required");

		try {
			var project = new Project()
				.name(name)
				.description(description)
				.user(user);
			db.insert(project);
			return  new CityGmlImport(db, project, gml).call();
		} catch (Exception e) {
			return Res.error("project creation failed", e);
		}
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
