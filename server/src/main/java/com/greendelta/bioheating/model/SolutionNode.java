package com.greendelta.bioheating.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Coordinate;

@Entity
@Table(name = "tbl_solution_nodes")
public class SolutionNode extends BaseEntity {

	@ManyToOne
	@JoinColumn(name = "f_building")
	private Building building;

	@Column(name = "x")
	private double x;

	@Column(name = "y")
	private double y;

	public Building building() {
		return building;
	}

	public SolutionNode building(Building building) {
		this.building = building;
		return this;
	}

	public double x() {
		return x;
	}

	public SolutionNode x(double x) {
		this.x = x;
		return this;
	}

	public double y() {
		return y;
	}

	public SolutionNode y(double y) {
		this.y = y;
		return this;
	}

	public boolean isBuildingNode() {
		return building != null;
	}

	public Coordinate center() {
		return new Coordinate(x, y);
	}
}
