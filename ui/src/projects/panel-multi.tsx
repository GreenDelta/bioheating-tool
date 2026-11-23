import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { GeoFeature, isBuilding } from "../model";
import { errorText } from "../i18n";
import * as api from "../api";
import { DownloadIcon } from "../components/icons";
import { NumberField, CheckboxField } from "./fields";
import { BuildingData } from "./panel-data";

interface Props {
	projectId: number;
	features: GeoFeature[];
	onChange: () => void;
}

export const MultiPanel: React.FC<Props> = ({ projectId, features, onChange }) => {
	return (
		<div className="card">
			<div className="card-body">
				<BuildingSection
					projectId={projectId}
					features={features}
					onChange={onChange}
				/>
			</div>
		</div>
	);
};

const BuildingSection = ({ projectId, features, onChange }: Props) => {
	const { t } = useTranslation();
	const buildings = features.filter(isBuilding);
	const [isIncluded, setIsIncluded] = useState(commonIsIncluded(buildings));
	const [isExporting, setIsExporting] = useState(false);
	const [error, setError] = useState<string | null>(null);
	if (buildings.length === 0) return <></>;
	const stats = buildingStatsOf(buildings);
	const onUpdate = (checked: boolean) => {
		putIsIncluded(buildings, checked);
		setIsIncluded(checked);
		onChange();
	};
	const onExport = async () => {
		if (isExporting) {
			return;
		}
		const ids = buildingIdsOf(buildings);
		if (ids.length === 0) {
			setError(t("multi.noExportable"));
			return;
		}

		setIsExporting(true);
		setError(null);
		const res = await api.exportProjectBuildingsXls(projectId, ids);
		if (!res.isOk) {
			setError(errorText(res.error));
		}
		setIsExporting(false);
	};

	return (
		<div className="mb-3">
			<h6 className="text-muted">
				{t("multi.summary", {
					count: buildings.length,
					heated: stats.heatedCount,
				})}
			</h6>
			{error && (
				<div className="alert alert-danger py-2" role="alert">
					{error}
				</div>
			)}
			<NumberField
				label={t("multi.totalHeatDemand")}
				readOnly
				value={stats.totalDemand}
			/>
			<CheckboxField
				label={t("multi.includeAll")}
				checked={isIncluded}
				onChange={onUpdate}
			/>
			<div className="row mt-3">
				<div className="col-sm-5"></div>
				<div className="col-sm-7 d-grid">
					<button
						type="button"
						className="btn btn-outline-primary"
						onClick={onExport}
						disabled={isExporting}>
						<DownloadIcon />
						{isExporting ? ` ${t("multi.exporting")}` : ` ${t("multi.export")}`}
					</button>
				</div>
			</div>
		</div>
	);
};

function buildingIdsOf(features: GeoFeature[]): number[] {
	const ids: number[] = [];
	for (const feature of features) {
		const id = feature.properties?.id;
		if (typeof id === "number") {
			ids.push(id);
		}
	}
	return ids;
}

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
