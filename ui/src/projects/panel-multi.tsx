import React, { useState } from "react";
import { GeoFeature, isBuilding } from "../model";
import { NumberField, CheckboxField } from "./fields";
import { BuildingData } from "./panel-data";

interface Props {
	features: GeoFeature[];
	onChange: () => void;
}

export const MultiPanel: React.FC<Props> = ({ features, onChange }) => {
	return (
		<div className="card">
			<div className="card-body">
				<BuildingSection features={features} onChange={onChange} />
			</div>
		</div>
	);
};

const BuildingSection = ({ features, onChange }: Props) => {
	const buildings = features.filter(isBuilding);
	if (!buildings || buildings.length === 0) return <></>;

	const [isIncluded, setIsIncluded] = useState(commonIsIncluded(buildings));
	const stats = buildingStatsOf(buildings);
	const onUpdate = (checked: boolean) => {
		putIsIncluded(buildings, checked);
		setIsIncluded(checked);
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
				value={stats.totalDemand}
			/>
			<CheckboxField
				label="Include all in solution"
				checked={isIncluded}
				onChange={onUpdate}
			/>
		</div>
	);
};

function putIsIncluded(features: GeoFeature[], value: boolean) {
	for (const f of features) {
		if (!f.properties) {
			f.properties = {};
		}
		f.properties.isIncluded = value;
	}
}

function commonIsIncluded(features: GeoFeature[]): boolean {
	if (!features || features.length === 0) {
		return false;
	}
	const first = features[0].properties?.isIncluded || false;
	for (let i = 1; i < features.length; i++) {
		const vi = features[i].properties?.isIncluded || false;
		if (vi !== first) {
			return false;
		}
	}
	return first;
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
