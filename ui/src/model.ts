export interface User {
	id: number;
	name: string;
	fullName: string;
	isAdmin: boolean;
}

export interface UserData {
	name: string;
	password: string;
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

export interface TaskState {
	status: TaskStatus;
	error?: string;
	result?: any;
}

export enum TaskStatus {
	RUNNING = "RUNNING",
	READY = "READY",
	ERROR = "ERROR",
}
