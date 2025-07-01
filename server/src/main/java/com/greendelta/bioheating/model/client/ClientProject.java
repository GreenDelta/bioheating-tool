package com.greendelta.bioheating.model.client;

import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.util.Res;

public record ClientProject(
	long id, String name, String description, ClientMap map
) {

	public static Res<ClientProject> of(Project project) {
		if (project == null)
			return Res.error("project is null");
		var map = MapConverter.toClient(project.map());
		if (map.hasError())
			return map.castError();
		var p = new ClientProject(
			project.id(),
			project.name(),
			project.description(),
			map.value()
		);
		return Res.of(p);
	}
}
