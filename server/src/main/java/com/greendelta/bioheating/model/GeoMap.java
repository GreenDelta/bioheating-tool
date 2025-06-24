package com.greendelta.bioheating.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbl_maps")
public class GeoMap extends BaseEntity {

	@Column(name = "crs")
	private String crs;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "f_map")
	private final List<Building> buildings = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "f_map")
	private final List<Street> streets = new ArrayList<>();

	public String crs() {
		return crs;
	}

	public GeoMap crs(String crs) {
		this.crs = crs;
		return this;
	}

	public List<Building> buildings() {
		return buildings;
	}

	public List<Street> streets() {
		return streets;
	}
}
