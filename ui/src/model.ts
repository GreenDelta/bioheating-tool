export interface User {
	name: string;
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

export interface Project {
	id: number;
	name: string;
	description: string;
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
