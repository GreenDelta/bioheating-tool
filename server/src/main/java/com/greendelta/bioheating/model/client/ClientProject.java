package com.greendelta.bioheating.model.client;

import com.greendelta.bioheating.model.ClimateRegion;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.model.Solution;
import org.openlca.commons.Res;

public record ClientProject(
	long id,
	String name,
	String description,
	ClimateRegion climateRegion,
	ClientMap map,
	Long solutionId
) {
	public static Res<ClientProject> of(Database db, Project project) {
		if (project == null) return Res.error("project is null");
		var map = ClientMap.of(project.map());
		if (map.isError()) return map.castError();
		var p = new ClientProject(
			project.id(),
			project.name(),
			project.description(),
			project.climateRegion(),
			map.value(),
			findSolutionId(db, project)
		);
		return Res.ok(p);
	}

	private static Long findSolutionId(Database db, Project project) {
		var solution = db
			.getAll(Solution.class)
			.stream()
			.filter(s -> project.equals(s.project()))
			.findAny()
			.orElse(null);
		return solution != null ? solution.id() : null;
	}

	public void writeUpdatesTo(Project project) {
		if (project == null) return;
		project.name(name);
		project.description(description);
		if (project.map() != null && map != null) {
			MapSync.updateFromClient(project.map(), map);
		}
	}
}
