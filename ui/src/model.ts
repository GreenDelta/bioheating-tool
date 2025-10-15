export interface User {
	id: number;
	name: string;
	fullName: string;
	isAdmin: boolean;
}

export interface UserData {
	name: string;
	password?: string;
	fullName: string;
	isAdmin: boolean;
}

export interface Credentials {
	user: string;
	password: string;
}

export interface ProjectInfo {
	id: number;
	name: string;
	description: string;
}

export interface ClimateRegion {
	id: number;
	number: number;
	name: string;
	stationName: string;
	stationId: string;
}

export interface Fuel {
	id: number;
	refId: string;
	name: string;
	unit: string;
	calorificValue: number;
}

export interface Project {
	id: number;
	name: string;
	description?: string;
	climateRegion?: ClimateRegion;
	defaultFuel?: Fuel;
	map: GeoMap;
	solutionId?: number;
}

export interface GeoMap {
	features: GeoFeature[];
}

export interface GeoFeature {
	type: "Feature";
	geometry: GeoPolygon | GeoLine;
	properties: { [key: string]: any };
}

export interface GeoPolygon {
	type: "Polygon";
	coordinates: number[][][];
}

export interface GeoLine {
	type: "LineString";
	coordinates: number[][];
}

export function isBuilding(f: GeoFeature): boolean {
	return f.properties && f.properties["@type"] === "building";
}

export function isStreet(f: GeoFeature): boolean {
	return f.properties && f.properties["@type"] == "street";
}

export enum Inclusion {
	OPTIONAL = "OPTIONAL",
	REQUIRED = "REQUIRED",
	EXCLUDED = "EXCLUDED",
}

export function inclusionFromString(value: string): Inclusion {
	switch (value?.toUpperCase()) {
		case "REQUIRED":
			return Inclusion.REQUIRED;
		case "EXCLUDED":
			return Inclusion.EXCLUDED;
		case "OPTIONAL":
		default:
			return Inclusion.OPTIONAL;
	}
}

export function inclusionToString(inclusion: Inclusion): string {
	return inclusion.toString();
}

export enum BuildingType {
	HIGH_RISE = "HIGH_RISE",
	MULTI_FAMILY_SMALL = "MULTI_FAMILY_SMALL",
	MULTI_FAMILY_MEDIUM = "MULTI_FAMILY_MEDIUM",
	MULTI_FAMILY_LARGE = "MULTI_FAMILY_LARGE",
	BUILDING_PART = "BUILDING_PART",
	SINGLE_FAMILY = "SINGLE_FAMILY",
	END_TERRACE = "END_TERRACE",
	MID_TERRACE = "MID_TERRACE",
	HOUSE_GROUP = "HOUSE_GROUP",
	OTHER = "OTHER",
}

export function buildingTypeFromString(value: string): BuildingType {
	switch (value?.toUpperCase()) {
		case "HIGH_RISE":
			return BuildingType.HIGH_RISE;
		case "MULTI_FAMILY_SMALL":
			return BuildingType.MULTI_FAMILY_SMALL;
		case "MULTI_FAMILY_MEDIUM":
			return BuildingType.MULTI_FAMILY_MEDIUM;
		case "MULTI_FAMILY_LARGE":
			return BuildingType.MULTI_FAMILY_LARGE;
		case "BUILDING_PART":
			return BuildingType.BUILDING_PART;
		case "SINGLE_FAMILY":
			return BuildingType.SINGLE_FAMILY;
		case "END_TERRACE":
			return BuildingType.END_TERRACE;
		case "MID_TERRACE":
			return BuildingType.MID_TERRACE;
		case "HOUSE_GROUP":
			return BuildingType.HOUSE_GROUP;
		case "OTHER":
		default:
			return BuildingType.OTHER;
	}
}

export interface TaskState {
	id: string;
	status: TaskStatus;
	error?: string;
	result?: any;
}

export interface Solution {
	id: number;
	name: string;
	projectId: number;
	calculatedAt: string;
}

export enum TaskStatus {
	RUNNING = "RUNNING",
	READY = "READY",
	ERROR = "ERROR",
}
