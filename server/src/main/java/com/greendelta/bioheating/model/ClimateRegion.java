package com.greendelta.bioheating.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbl_climate_regions")
public class ClimateRegion extends BaseEntity {

	@JsonProperty
	@Column(name = "number")
	private int number;

	@JsonProperty
	@Column(name = "name")
	private String name;

	@JsonProperty
	@Column(name = "station_name")
	private String stationName;

	@JsonProperty
	@Column(name = "station_id")
	private String stationId;

	public int number() {
		return number;
	}

	public ClimateRegion number(int number) {
		this.number = number;
		return this;
	}

	public String name() {
		return name;
	}

	public ClimateRegion name(String name) {
		this.name = name;
		return this;
	}

	public String stationName() {
		return stationName;
	}

	public ClimateRegion stationName(String stationName) {
		this.stationName = stationName;
		return this;
	}

	public String stationId() {
		return stationId;
	}

	public ClimateRegion stationId(String stationId) {
		this.stationId = stationId;
		return this;
	}

	@Override
	public String toString() {
		return "ClimateRegion [ name=" + name + " ]";
	}
}
