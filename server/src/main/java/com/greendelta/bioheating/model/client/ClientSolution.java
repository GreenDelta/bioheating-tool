package com.greendelta.bioheating.model.client;

import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.model.Solution;

public record ClientSolution(
	long id,
	String name,
	long projectId
) {

	public static ClientSolution of(Solution s) {
		var p = s.project();
		var name = p != null ? p.name() : "No project";
		long projectId = p != null ? p.id() : -1;
		return new ClientSolution(s.id(), name, projectId);
	}

}
