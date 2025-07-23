package com.greendelta.bioheating.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbl_fuels")
public class Fuel extends BaseEntity {

	@JsonProperty
	@Column(name = "ref_id")
	private String refId;

	@JsonProperty
	@Column(name = "name")
	private String name;

	@JsonProperty
	@Column(name = "unit")
	private String unit;

	@JsonProperty
	@Column(name = "calorific_value")
	private double calorificValue;

	public String refId() {
		return refId;
	}

	public Fuel refId(String refId) {
		this.refId = refId;
		return this;
	}

	public String name() {
		return name;
	}

	public Fuel name(String name) {
		this.name = name;
		return this;
	}

	public String unit() {
		return unit;
	}

	public Fuel unit(String unit) {
		this.unit = unit;
		return this;
	}

	public double calorificValue() {
		return calorificValue;
	}

	public Fuel calorificValue(double calorificValue) {
		this.calorificValue = calorificValue;
		return this;
	}

	@Override
	public String toString() {
		return "Fuel [ name=" + name + ", unit=" + unit + " ]";
	}
}
