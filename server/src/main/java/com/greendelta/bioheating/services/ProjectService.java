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

	public Res<Project> addMap(Project project, File gml) {
		if (project == null || gml == null)
			return Res.error("no project or gml file provided");
		try {
			return new CityGmlImport(db, project, gml)
				.withOsmImport(true)
				.call();
		} catch (Exception e) {
			return Res.error("project creation failed", e);
		}
	}

	public Res<Void> delete(Project project) {
		if (project == null)
			return Res.error("no project given");
		try {
			db.delete(project);
			return Res.VOID;
		} catch (Exception e) {
			return Res.error("failed to delete project", e);
		}
	}

	public Res<Project> updateProject(Project project) {
		if (project == null)
			return Res.error("project is null");
		try {
			db.update(project);
			return Res.of(project);
		} catch (Exception e) {
			return Res.error("failed to save project", e);
		}
	}
}
