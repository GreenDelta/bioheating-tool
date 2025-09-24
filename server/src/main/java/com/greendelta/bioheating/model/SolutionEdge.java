package com.greendelta.bioheating.model;

import org.locationtech.jts.geom.Coordinate;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbl_solution_edges")
public class SolutionEdge extends BaseEntity {

	@ManyToOne
	@JoinColumn(name = "f_source_node")
	private SolutionNode source;

	@ManyToOne
	@JoinColumn(name = "f_target_node")
	private SolutionNode target;

	@Column(name = "length")
	private double length;

	@Column(name = "coordinates")
	@Convert(converter = CoordinateConverter.class)
	private Coordinate[] coordinates;

	public SolutionNode source() {
		return source;
	}

	public SolutionEdge source(SolutionNode source) {
		this.source = source;
		return this;
	}

	public SolutionNode target() {
		return target;
	}

	public SolutionEdge target(SolutionNode target) {
		this.target = target;
		return this;
	}

	public double length() {
		return length;
	}

	public SolutionEdge length(double length) {
		this.length = length;
		return this;
	}

	public Coordinate[] coordinates() {
		return coordinates;
	}

	public SolutionEdge coordinates(Coordinate[] coordinates) {
		this.coordinates = coordinates;
		return this;
	}
}
