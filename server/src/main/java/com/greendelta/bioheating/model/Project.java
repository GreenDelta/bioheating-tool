package com.greendelta.bioheating.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbl_projects")
public class Project extends BaseEntity {

	private String name;
	private String description;

	public String name() {
		return name;
	}

	public Project name(String name) {
		this.name = name;
		return this;
	}

	public String description() {
		return description;
	}

	public Project description(String description) {
		this.description = description;
		return this;
	}

	@Override
	public String toString() {
		return "Project [id=" + id() + ", name=" + name
			+ ", description=" + description + "]";
	}
}
