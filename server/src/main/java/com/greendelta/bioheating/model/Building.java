package com.greendelta.bioheating.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Coordinate;

@Entity
@Table(name = "tbl_buildings")
public class Building extends BaseEntity {

	@Column(name = "name")
	private String name;

	@Column(name = "city_id")
	private String cityId;

	/// The coordinates of the building ground surface in UTM.
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
	@Enumerated(EnumType.STRING)
	private ConstructionAge constructionAge;

	@Column(name = "height")
	private double height;

	@Column(name = "storeys")
	private int storeys;

	@Column(name = "ground_area")
	private double groundArea;

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

	@Column(name = "peak_load")
	private double peakLoad;

	@Column(name = "is_heated")
	private boolean isHeated;

	@Column(name = "is_supply_center")
	private boolean isSupplyCenter;

	@Column(name = "is_included")
	private boolean isIncluded;

	public String name() {
		return name;
	}

	public Building name(String name) {
		this.name = name;
		return this;
	}

	public String cityId() {
		return cityId;
	}

	public Building cityId(String cityId) {
		this.cityId = cityId;
		return this;
	}

	/// The coordinates of the building ground surface in UTM.
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

	public ConstructionAge constructionAge() {
		return constructionAge;
	}

	public Building constructionAge(ConstructionAge constructionAge) {
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

	/// Returns the annual heat demand of the building.
	///
	/// @return the heat demand in kWh/year
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

	public boolean isSupplyCenter() {
		return isSupplyCenter;
	}

	public Building isSupplyCenter(boolean isSupplyCenter) {
		this.isSupplyCenter = isSupplyCenter;
		return this;
	}

	public boolean isIncluded() {
		return isIncluded;
	}

	public Building isIncluded(boolean isIncluded) {
		this.isIncluded = isIncluded;
		return this;
	}

	/// Returns the peak heating load of the building in kW.
	public double peakLoad() {
		return peakLoad;
	}

	public Building peakLoad(double peakLoad) {
		this.peakLoad = peakLoad;
		return this;
	}

	@Override
	public String toString() {
		return "Building [id=" + id() + ", name=" + name + "]";
	}
}
