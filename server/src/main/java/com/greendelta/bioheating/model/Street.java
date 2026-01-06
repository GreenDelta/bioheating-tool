package com.greendelta.bioheating.model;

import org.locationtech.jts.geom.Coordinate;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbl_streets")
public class Street extends BaseEntity {

	@Column(name = "name")
	private String name;

	@Column(name = "coordinates")
	@Convert(converter = CoordinateConverter.class)
	private Coordinate[] coordinates;

	@Column(name = "is_excluded")
	private boolean isExcluded;

	public String name() {
		return name;
	}

	public Street name(String name) {
		this.name = name;
		return this;
	}

	public Coordinate[] coordinates() {
		return coordinates;
	}

	public Street coordinates(Coordinate[] coordinates) {
		this.coordinates = coordinates;
		return this;
	}

	public boolean isExcluded() {
		return isExcluded;
	}

	public Street isExcluded(boolean isExcluded) {
		this.isExcluded = isExcluded;
		return this;
	}
}
