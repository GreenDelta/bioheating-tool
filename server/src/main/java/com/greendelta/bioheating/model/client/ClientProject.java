package com.greendelta.bioheating.model.client;

import com.greendelta.bioheating.model.ClimateRegion;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Fuel;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.util.Res;

public record ClientProject(
	long id,
	String name,
	String description,
	ClimateRegion climateRegion,
	Fuel defaultFuel,
	ClientMap map
) {

	public static Res<ClientProject> of(Project project) {
		if (project == null)
			return Res.error("project is null");
		var map = ClientMap.of(project.map());
		if (map.hasError())
			return map.castError();
		var p = new ClientProject(
			project.id(),
			project.name(),
			project.description(),
			project.climateRegion(),
			project.defaultFuel(),
			map.value()
		);
		return Res.of(p);
	}

	public void writeUpdatesTo(Database db, Project project) {
		if (project == null)
			return;
		project.name(name);
		project.description(description);
		if (project.map() != null && map != null) {
			MapSync.updateFromClient(db, project.map(), map);
		}
	}

}
