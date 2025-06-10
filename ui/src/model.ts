export interface User {
	name: string;
	fullName: string;
	isAdmin: boolean;
}

export interface Credentials {
	user: string;
	password: string;
}

export interface Project {
	id: number;
	name: string;
	description: string;
	cityGmlFileName?: string;
}

export interface ProjectData {
	name: string;
	description: string;
	cityGmlFileName?: string;
}

// GeoJSON types
export interface GeoJSONFeature {
	type: 'Feature';
	geometry: GeoJSONGeometry;
	properties: { [key: string]: any };
}

export interface GeoJSONFeatureCollection {
	type: 'FeatureCollection';
	features: GeoJSONFeature[];
}

export interface GeoJSONGeometry {
	type: 'Point' | 'LineString' | 'Polygon' | 'MultiPoint' | 'MultiLineString' | 'MultiPolygon';
	coordinates: number[] | number[][] | number[][][];
}

export interface BuildingProperties {
	id: number;
	name?: string;
	type: 'building';
}
