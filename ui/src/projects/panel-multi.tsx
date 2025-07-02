import React, { useState } from 'react';
import { GeoFeature, Inclusion, isBuilding, isStreet } from '../model';
import { NumberField, SelectField } from './fields';
import { BuildingData } from './panel-data';

interface Props {
	features: GeoFeature[];
	onChange: () => void;
}

export const MultiPanel: React.FC<Props> = ({ features, onChange }) => {
	return (
		<div className="card">
			<div className="card-body">
				<BuildingSection features={features} onChange={onChange} />
				<StreetSection features={features} onChange={onChange} />
			</div>
		</div>
	);
};

const BuildingSection = ({ features, onChange }: Props) => {
	const buildings = features.filter(isBuilding);
	if (!buildings || buildings.length === 0)
		return <></>;

	const [inclusion, setInclusion] = useState(commonInclusionOf(buildings));
	const stats = buildingStatsOf(buildings);
	const onUpdate = (v: string) => {
		const next = v === "" ? Inclusion.EXCLUDED : v;
		putInclusion(buildings, next);
		setInclusion(next);
		onChange();
	};

	return (
		<div className="mb-3">
			<h6 className="text-muted">
				{buildings.length} Buildings, {stats.heatedCount} heated
			</h6>
			<NumberField
				label="Total heat demand [kWh/a]"
				readOnly
				value={stats.totalDemand} />
			<SelectField
				label="Set inclusion"
				value={inclusion}
				options={[
					{ value: '', label: 'Select...' },
					{ value: Inclusion.REQUIRED, label: 'Included' },
					{ value: Inclusion.EXCLUDED, label: 'Excluded' }
				]}
				onChange={onUpdate}
			/>
		</div>
	);
};

const StreetSection = ({ features, onChange }: Props) => {
	const streets = features.filter(isStreet);
	if (!streets || streets.length === 0)
		return <></>;

	const [inclusion, setInclusion] = useState(commonInclusionOf(streets));
	const onUpdate = (v: string) => {
		const next = v === "" ? Inclusion.OPTIONAL : v;
		putInclusion(streets, next);
		setInclusion(next);
		onChange();
	};

	return (
		<div className="mb-3">
			<h6 className="text-muted">{streets.length} Streets</h6>
			<SelectField
				label="Set inclusion"
				value={inclusion}
				options={[
					{ value: '', label: 'Select...' },
					{ value: Inclusion.OPTIONAL, label: 'Optional' },
					{ value: Inclusion.REQUIRED, label: 'Required' },
					{ value: Inclusion.EXCLUDED, label: 'Excluded' }
				]}
				onChange={onUpdate}
			/>
		</div>
	);
}

function putInclusion(features: GeoFeature[], value: string) {
	for (const f of features) {
		if (!f.properties) {
			f.properties = {};
		}
		f.properties.inclusion = value;
	}
}

function commonInclusionOf(features: GeoFeature[]): string {
	if (!features || features.length === 0) {
		return "";
	}
	let v = features[0].properties?.inclusion || "";
	for (let i = 1; i < features.length; i++) {
		const vi = features[i].properties?.inclusion;
		if (vi !== v) {
			return "";
		}
	}
	return v;
}

interface BuildingStats {
	heatedCount: number;
	totalDemand: number;
}

function buildingStatsOf(features: GeoFeature[]): BuildingStats {
	let heatedCount = 0;
	let totalDemand = 0;
	if (!features || features.length === 0) {
		return { heatedCount, totalDemand };
	}
	for (const f of features) {
		const data = BuildingData.of(f);
		if (!data.isHeated) {
			continue;
		}
		totalDemand += data.heatDemand;
		heatedCount++;
	}
	totalDemand = Math.round(totalDemand);
	return { heatedCount, totalDemand };
}
