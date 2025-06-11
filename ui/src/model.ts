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
	geometry: GeoPolygon;
	properties: { [key: string]: any };
}

export interface GeoPolygon {
	type: "Polygon";
	coordinates: number[][][];
}
