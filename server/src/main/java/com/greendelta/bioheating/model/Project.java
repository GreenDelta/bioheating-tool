package com.greendelta.bioheating.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbl_projects")
public class Project extends BaseEntity {

	@Column(name = "name")
	private String name;

	@Column(name = "description")
	private String description;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "f_map")
	private GeoMap map;

	@ManyToOne
	@JoinColumn(name = "f_user")
	private User user;

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

	public User user() {
		return user;
	}

	public Project user(User user) {
		this.user = user;
		return this;
	}

	@Override
	public String toString() {
		return "Project [id=" + id() + ", name=" + name + "]";
	}
}
