package com.greendelta.bioheating.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbl_solutions")
public class Solution extends BaseEntity {

	@ManyToOne
	@JoinColumn(name = "f_project")
	private Project project;

	@Column(name = "image")
	private byte[] image;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "f_solution")
	private final List<SolutionNode> nodes = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "f_solution")
	private final List<SolutionEdge> edges = new ArrayList<>();

	public Project project() {
		return project;
	}

	public Solution project(Project project) {
		this.project = project;
		return this;
	}

	public byte[] image() {
		return image;
	}

	public Solution image(byte[] image) {
		this.image = image;
		return this;
	}

	public List<SolutionNode> nodes() {
		return nodes;
	}

	public List<SolutionEdge> edges() {
		return edges;
	}
}
