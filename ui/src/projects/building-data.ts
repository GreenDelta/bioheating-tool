import { GeoFeature } from "../model";

interface Props {
	name?: any;
	height?: any;
	storeys?: any;
	heatDemand?: any;
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

	static of(f: GeoFeature): BuildingData {
		const props = f.properties || {};
		return new BuildingData(props);
	}

	constructor(d: BuildingData | Props) {
		if (d instanceof BuildingData) {
			this.name = d.name;
			this.height = d.height;
			this.storeys = d.storeys;
			this.heatDemand = d.heatDemand;
		} else {
			this.name = stringOf(d.name);
			this.height = floatOf(d.height);
			this.storeys = intOf(d.storeys);
			this.heatDemand = floatOf(d.heatDemand);
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
	}

	isValid(): boolean {
		return (typeof this.name === "string") && this.name.trim().length > 0;
	}
}
