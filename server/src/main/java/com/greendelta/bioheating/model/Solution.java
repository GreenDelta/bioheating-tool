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

	@Column(name = "calculated_at")
	private long calculatedAt;

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

	public long calculatedAt() {
		return calculatedAt;
	}

	public Solution calculatedAt(long calculatedAt) {
		this.calculatedAt = calculatedAt;
		return this;
	}

	public List<SolutionNode> nodes() {
		return nodes;
	}

	public List<SolutionEdge> edges() {
		return edges;
	}

	/// Assigns transient IDs to the nodes and edges of this solution if they are
	/// 0. This is useful for testing or in-memory processing where the solution
	// is not  persisted to the database.
	public Solution withTransientIds() {
		long nextId = 1;
		for (var node : nodes) {
			if (node.id() == 0) {
				node.id(nextId++);
			}
		}
		nextId = 1;
		for (var edge : edges) {
			if (edge.id() == 0) {
				edge.id(nextId++);
			}
		}
		return this;
	}
}
