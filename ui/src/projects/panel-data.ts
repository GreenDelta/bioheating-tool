import { GeoFeature } from "../model";
import { Inclusion, inclusionFromString, BuildingType, buildingTypeFromString } from "../model";

export interface BuildingProps {
	name?: any;
	height?: any;
	storeys?: any;
	heatDemand?: any;
	roofTypeCode?: any;
	roofTypeLabel?: any;
	roofType?: any; // backward compatibility
	functionCode?: any;
	functionLabel?: any;
	function?: any; // backward compatibility
	type?: any;
	constructionAge?: any;
	groundArea?: any;
	heatedArea?: any;
	volume?: any;
	country?: any;
	locality?: any;
	postalCode?: any;
	street?: any;
	streetNumber?: any;
	isHeated?: any;
	inclusion?: any;
	fuelId?: any;
}

function stringOf(field: any): string {
	return typeof field === "string" ? field : "";
}

function floatOf(field: any): number {
	const t = typeof field;
	if (t === "number") {
		return field;
	}
	if (t === "string") {
		const f = parseFloat(field);
		return isNaN(f) ? 0 : f;
	}
	return 0;
}

function intOf(field: any): number {
	const t = typeof field;
	if (t === "number") {
		return field;
	}
	if (t === "string") {
		const i = parseInt(field);
		return isNaN(i) ? 0 : i;
	}
	return 0;
}

function boolOf(field: any): boolean {
	const t = typeof field;
	if (t === "boolean") {
		return field;
	}
	if (t === "string") {
		return field.toLowerCase() === "true";
	}
	return false;
}

export class BuildingData {
	name: string;
	height: number;
	storeys: number;
	heatDemand: number;
	roofTypeCode: string;
	roofTypeLabel: string;
	functionCode: string;
	functionLabel: string;
	type: BuildingType;
	constructionAge: string;
	groundArea: number;
	heatedArea: number;
	volume: number;
	country: string;
	locality: string;
	postalCode: string;
	street: string;
	streetNumber: string;
	isHeated: boolean;
	inclusion: Inclusion;
	fuelId?: number;

	static of(f: GeoFeature): BuildingData {
		const props = f.properties || {};
		return new BuildingData(props);
	}

	constructor(d: BuildingData | BuildingProps) {
		if (d instanceof BuildingData) {
			this.name = d.name;
			this.height = d.height;
			this.storeys = d.storeys;
			this.heatDemand = d.heatDemand;
			this.roofTypeCode = d.roofTypeCode;
			this.roofTypeLabel = d.roofTypeLabel;
			this.functionCode = d.functionCode;
			this.functionLabel = d.functionLabel;
			this.type = d.type;
			this.constructionAge = d.constructionAge;
			this.groundArea = d.groundArea;
			this.heatedArea = d.heatedArea;
			this.volume = d.volume;
			this.country = d.country;
			this.locality = d.locality;
			this.postalCode = d.postalCode;
			this.street = d.street;
			this.streetNumber = d.streetNumber;
			this.isHeated = d.isHeated;
			this.inclusion = d.inclusion;
			this.fuelId = d.fuelId;
		} else {
			this.name = stringOf(d.name);
			this.height = floatOf(d.height);
			this.storeys = intOf(d.storeys);
			this.heatDemand = floatOf(d.heatDemand);
			// Handle backward compatibility - if old properties exist, use them as codes
			this.roofTypeCode = stringOf(d.roofTypeCode || d.roofType);
			this.roofTypeLabel = stringOf(d.roofTypeLabel || d.roofType);
			this.functionCode = stringOf(d.functionCode || d.function);
			this.functionLabel = stringOf(d.functionLabel || d.function);
			this.type = buildingTypeFromString(d.type || "OTHER");
			this.constructionAge = stringOf(d.constructionAge);
			this.groundArea = floatOf(d.groundArea);
			this.heatedArea = floatOf(d.heatedArea);
			this.volume = floatOf(d.volume);
			this.country = stringOf(d.country);
			this.locality = stringOf(d.locality);
			this.postalCode = stringOf(d.postalCode);
			this.street = stringOf(d.street);
			this.streetNumber = stringOf(d.streetNumber);
			this.isHeated = boolOf(d.isHeated);
			this.inclusion = inclusionFromString(d.inclusion || "REQUIRED");
			this.fuelId = d.fuelId ? intOf(d.fuelId) : undefined;
		}
	}

	copyWith(props: BuildingProps): BuildingData {
		const copy = new BuildingData(this);
		if (props.name) {
			copy.name = props.name;
		}
		if (props.height) {
			copy.height = props.height;
		}
		if (props.storeys) {
			copy.storeys = props.storeys;
		}
		if (props.heatDemand) {
			copy.heatDemand = props.heatDemand;
		}
		if (props.roofTypeCode) {
			copy.roofTypeCode = props.roofTypeCode;
		}
		if (props.roofTypeLabel) {
			copy.roofTypeLabel = props.roofTypeLabel;
		}
		if (props.functionCode) {
			copy.functionCode = props.functionCode;
		}
		if (props.functionLabel) {
			copy.functionLabel = props.functionLabel;
		}
		if (props.type) {
			copy.type = buildingTypeFromString(props.type);
		}
		if (props.constructionAge) {
			copy.constructionAge = props.constructionAge;
		}
		if (props.groundArea) {
			copy.groundArea = props.groundArea;
		}
		if (props.heatedArea) {
			copy.heatedArea = props.heatedArea;
		}
		if (props.volume) {
			copy.volume = props.volume;
		}
		if (props.country) {
			copy.country = props.country;
		}
		if (props.locality) {
			copy.locality = props.locality;
		}
		if (props.postalCode) {
			copy.postalCode = props.postalCode;
		}
		if (props.street) {
			copy.street = props.street;
		}
		if (props.streetNumber) {
			copy.streetNumber = props.streetNumber;
		}
		if (props.isHeated !== undefined) {
			copy.isHeated = props.isHeated;
		}
		if (props.inclusion !== undefined) {
			copy.inclusion = inclusionFromString(props.inclusion);
		}
		if (props.fuelId !== undefined) {
			copy.fuelId = props.fuelId ? intOf(props.fuelId) : undefined;
		}
		return copy;
	}

	applyOn(f: GeoFeature) {
		if (!f.properties) {
			f.properties = {};
		}
		f.properties.name = this.name;
		f.properties.height = this.height;
		f.properties.storeys = this.storeys;
		f.properties.heatDemand = this.heatDemand;
		f.properties.roofTypeCode = this.roofTypeCode;
		f.properties.roofTypeLabel = this.roofTypeLabel;
		f.properties.functionCode = this.functionCode;
		f.properties.functionLabel = this.functionLabel;
		f.properties.type = this.type;
		f.properties.constructionAge = this.constructionAge;
		f.properties.groundArea = this.groundArea;
		f.properties.heatedArea = this.heatedArea;
		f.properties.volume = this.volume;
		f.properties.country = this.country;
		f.properties.locality = this.locality;
		f.properties.postalCode = this.postalCode;
		f.properties.street = this.street;
		f.properties.streetNumber = this.streetNumber;
		f.properties.isHeated = this.isHeated;
		f.properties.inclusion = this.inclusion;
		if (this.fuelId !== undefined) {
			f.properties.fuelId = this.fuelId;
		}
	}

	isValid(): boolean {
		return typeof this.name === "string" && this.name.trim().length > 0;
	}
}

export interface StreetProps {
	name?: any;
	inclusion?: any;
}

export class StreetData {
	name: string;
	inclusion: Inclusion;

	static of(f: GeoFeature): StreetData {
		const props = f.properties || {};
		return new StreetData(props);
	}

	constructor(d: StreetData | StreetProps) {
		if (d instanceof StreetData) {
			this.name = d.name;
			this.inclusion = d.inclusion;
		} else {
			this.name = stringOf(d.name);
			this.inclusion = inclusionFromString(d.inclusion);
		}
	}

	copyWith(props: StreetProps): StreetData {
		const copy = new StreetData(this);
		if (props.name) {
			copy.name = props.name;
		}
		if (props.inclusion !== undefined) {
			copy.inclusion = inclusionFromString(props.inclusion);
		}
		return copy;
	}

	applyOn(f: GeoFeature) {
		if (!f.properties) {
			f.properties = {};
		}
		f.properties.name = this.name;
		f.properties.inclusion = this.inclusion;
	}

	isValid(): boolean {
		return typeof this.name === "string" && this.name.trim().length > 0;
	}
}
