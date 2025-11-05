package com.greendelta.bioheating.model;

import org.locationtech.jts.geom.Coordinate;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbl_buildings")
public class Building extends BaseEntity {

	@Column(name = "name")
	private String name;

	@Column(name = "coordinates")
	@Convert(converter = CoordinateConverter.class)
	private Coordinate[] coordinates;

	@Column(name = "roof_type_code")
	private String roofTypeCode;

	@Column(name = "roof_type_label")
	private String roofTypeLabel;

	@Column(name = "function_code")
	private String functionCode;

	@Column(name = "function_label")
	private String functionLabel;

	@Column(name = "building_type")
	@Enumerated(EnumType.STRING)
	private BuildingType type;

	@Column(name = "construction_age")
	private String constructionAge;

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

	@Column(name = "heat_demand")
	private double heatDemand;

	@Column(name = "is_heated")
	private boolean isHeated;

	@Column(name = "inclusion")
	@Enumerated(EnumType.STRING)
	private Inclusion inclusion;

	@ManyToOne
	@JoinColumn(name = "f_fuel")
	private Fuel fuel;

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

	public String roofTypeCode() {
		return roofTypeCode;
	}

	public Building roofTypeCode(String roofTypeCode) {
		this.roofTypeCode = roofTypeCode;
		return this;
	}

	public String roofTypeLabel() {
		return roofTypeLabel;
	}

	public Building roofTypeLabel(String roofTypeLabel) {
		this.roofTypeLabel = roofTypeLabel;
		return this;
	}

	public String functionCode() {
		return functionCode;
	}

	public Building functionCode(String functionCode) {
		this.functionCode = functionCode;
		return this;
	}

	public String functionLabel() {
		return functionLabel;
	}

	public Building functionLabel(String functionLabel) {
		this.functionLabel = functionLabel;
		return this;
	}

	public BuildingType type() {
		return type;
	}

	public Building type(BuildingType type) {
		this.type = type;
		return this;
	}

	public String constructionAge() {
		return constructionAge;
	}

	public Building constructionAge(String constructionAge) {
		this.constructionAge = constructionAge;
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

	public Inclusion inclusion() {
		return inclusion;
	}

	public Building inclusion(Inclusion inclusion) {
		this.inclusion = inclusion;
		return this;
	}

	public Fuel fuel() {
		return fuel;
	}

	public Building fuel(Fuel fuel) {
		this.fuel = fuel;
		return this;
	}

	@Override
	public String toString() {
		return "Building [id=" + id() + ", name=" + name + "]";
	}
}
