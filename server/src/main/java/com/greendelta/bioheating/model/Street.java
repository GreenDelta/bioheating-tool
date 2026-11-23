package com.greendelta.bioheating.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Coordinate;

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

	/// The ID of the OSM way from which the street was imported. It is used to
	/// only add new streets when the streets of a project are fetched again.
	@Column(name = "osm_id")
	private long osmId;

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

	public long osmId() {
		return osmId;
	}

	public Street osmId(long osmId) {
		this.osmId = osmId;
		return this;
	}
}
