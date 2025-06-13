import { GeoFeature } from "../model";

interface Props {
	name?: any;
	height?: any;
	storeys?: any;
	heatDemand?: any;
	roofType?: any;
	function?: any;
	groundArea?: any;
	heatedArea?: any;
	volume?: any;
	country?: any;
	locality?: any;
	postalCode?: any;
	street?: any;
	streetNumber?: any;
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

export class BuildingData {

	name: string;
	height: number;
	storeys: number;
	heatDemand: number;
	roofType: string;
	function: string;
	groundArea: number;
	heatedArea: number;
	volume: number;
	country: string;
	locality: string;
	postalCode: string;
	street: string;
	streetNumber: string;

	static of(f: GeoFeature): BuildingData {
		const props = f.properties || {};
		return new BuildingData(props);
	}	constructor(d: BuildingData | Props) {
		if (d instanceof BuildingData) {
			this.name = d.name;
			this.height = d.height;
			this.storeys = d.storeys;
			this.heatDemand = d.heatDemand;
			this.roofType = d.roofType;
			this.function = d.function;
			this.groundArea = d.groundArea;
			this.heatedArea = d.heatedArea;
			this.volume = d.volume;
			this.country = d.country;
			this.locality = d.locality;
			this.postalCode = d.postalCode;
			this.street = d.street;
			this.streetNumber = d.streetNumber;
		} else {
			this.name = stringOf(d.name);
			this.height = floatOf(d.height);
			this.storeys = intOf(d.storeys);
			this.heatDemand = floatOf(d.heatDemand);
			this.roofType = stringOf(d.roofType);
			this.function = stringOf(d.function);
			this.groundArea = floatOf(d.groundArea);
			this.heatedArea = floatOf(d.heatedArea);
			this.volume = floatOf(d.volume);
			this.country = stringOf(d.country);
			this.locality = stringOf(d.locality);
			this.postalCode = stringOf(d.postalCode);
			this.street = stringOf(d.street);
			this.streetNumber = stringOf(d.streetNumber);
		}
	}
	copyWith(props: Props): BuildingData {
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
		if (props.roofType) {
			copy.roofType = props.roofType;
		}
		if (props.function) {
			copy.function = props.function;
		}
		if (props.groundArea) {
			copy.groundArea = props.groundArea;
		}
		if (props.heatedArea) {
			copy.heatedArea = props.heatedArea;
		}		if (props.volume) {
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
		f.properties.roofType = this.roofType;
		f.properties.function = this.function;		f.properties.groundArea = this.groundArea;
		f.properties.heatedArea = this.heatedArea;
		f.properties.volume = this.volume;
		f.properties.country = this.country;
		f.properties.locality = this.locality;
		f.properties.postalCode = this.postalCode;
		f.properties.street = this.street;
		f.properties.streetNumber = this.streetNumber;
	}

	isValid(): boolean {
		return (typeof this.name === "string") && this.name.trim().length > 0;
	}
}
