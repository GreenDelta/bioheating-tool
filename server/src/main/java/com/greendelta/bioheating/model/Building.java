package com.greendelta.bioheating.model;

import org.locationtech.jts.geom.Coordinate;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbl_buildings")
public class Building extends BaseEntity {

	@Column(name = "name")
	private String name;

	@Column(name = "coordinates")
	@Convert(converter = CoordinateConverter.class)
	private Coordinate[] coordinates;

	@Column(name = "roof_type")
	private String roofType;

	@Column(name = "function")
	private String function;

	@Column(name = "height")
	private double height;

	@Column(name = "storeys")
	private int storeys;

	@Column(name = "ground_area")
	private double groundArea;

	@Column(name = "heated_area")
	private double heatedArea;

	@Column(name = "volume")
	private double volume;

	@Column(name = "country")
	private String country;

	@Column(name = "locality")
	private String locality;

	@Column(name = "postal_code")
	private String postalCode;

	@Column(name = "street")
	private String street;

	@Column(name = "street_number")
	private String streetNumber;
	@Column(name = "climate_zone")
	private int climateZone;

	@Column(name = "heat_demand")
	private double heatDemand;

	@Column(name = "is_heated")
	private boolean isHeated;

	@Column(name = "is_included")
	private boolean isIncluded;

	public String name() {
		return name;
	}

	public Building name(String name) {
		this.name = name;
		return this;
	}

	public Coordinate[] coordinates() {
		return coordinates;
	}

	public Building coordinates(Coordinate[] coordinates) {
		this.coordinates = coordinates;
		return this;
	}

	public String roofType() {
		return roofType;
	}

	public Building roofType(String roofType) {
		this.roofType = roofType;
		return this;
	}

	public String function() {
		return function;
	}

	public Building function(String function) {
		this.function = function;
		return this;
	}

	public double height() {
		return height;
	}

	public Building height(double height) {
		this.height = height;
		return this;
	}

	public int storeys() {
		return storeys;
	}

	public Building storeys(int storeys) {
		this.storeys = storeys;
		return this;
	}

	public double groundArea() {
		return groundArea;
	}

	public Building groundArea(double groundArea) {
		this.groundArea = groundArea;
		return this;
	}

	public double heatedArea() {
		return heatedArea;
	}

	public Building heatedArea(double heatedArea) {
		this.heatedArea = heatedArea;
		return this;
	}

	public double volume() {
		return volume;
	}

	public Building volume(double volume) {
		this.volume = volume;
		return this;
	}

	public String country() {
		return country;
	}

	public Building country(String country) {
		this.country = country;
		return this;
	}

	public String locality() {
		return locality;
	}

	public Building locality(String locality) {
		this.locality = locality;
		return this;
	}

	public String postalCode() {
		return postalCode;
	}

	public Building postalCode(String postalCode) {
		this.postalCode = postalCode;
		return this;
	}

	public String street() {
		return street;
	}

	public Building street(String street) {
		this.street = street;
		return this;
	}

	public String streetNumber() {
		return streetNumber;
	}

	public Building streetNumber(String streetNumber) {
		this.streetNumber = streetNumber;
		return this;
	}

	public int climateZone() {
		return climateZone;
	}
	public Building climateZone(int climateZone) {
		this.climateZone = climateZone;
		return this;
	}

	public double heatDemand() {
		return heatDemand;
	}

	public Building heatDemand(double heatDemand) {
		this.heatDemand = heatDemand;
		return this;
	}

	public boolean isHeated() {
		return isHeated;
	}

	public Building isHeated(boolean isHeated) {
		this.isHeated = isHeated;
		return this;
	}

	public boolean isIncluded() {
		return isIncluded;
	}

	public Building isIncluded(boolean isIncluded) {
		this.isIncluded = isIncluded;
		return this;
	}

	@Override
	public String toString() {
		return "Building [id=" + id() + ", name=" + name + "]";
	}
}
