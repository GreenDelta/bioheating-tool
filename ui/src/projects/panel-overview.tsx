import React from "react";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { formatNumber } from "../i18n";
import { Project, isBuilding } from "../model";
import { BuildingData } from "./panel-data";

interface Props {
	project: Project;
	isDirty: boolean;
	onChange: (change: Partial<Project>) => void;
}

function keyFiguresOf(project: Project) {
	const fs = project?.map?.features || [];
	let buildingCount = 0;
	let totalDemand = 0;
	for (const f of fs) {
		if (!isBuilding(f)) {
			continue;
		}
		const data = BuildingData.of(f);
		if (data.isIncluded && data.isHeated) {
			buildingCount++;
			totalDemand += data.heatDemand;
		}
	}
	return { buildingCount, totalDemand };
}

export const OverviewPanel = ({ project, isDirty, onChange }: Props) => {
	const { t } = useTranslation();
	const { buildingCount, totalDemand } = keyFiguresOf(project);

	const formatHeatDemand = (value: number): string => {
		const fmt = (v: number) => formatNumber(v, { maximumFractionDigits: 0 });
		if (value >= 1000000) {
			return `${fmt(value / 1000000)} GWh`;
		} else if (value >= 1000) {
			return `${fmt(value / 1000)} MWh`;
		}
		return `${fmt(value)} kWh`;
	};

	return (
		<div className="card">
			<div className="card-header">
				<h5 className="card-title mb-0">{t("overview.title")}</h5>
			</div>
			<div className="card-body">
				<div className="d-flex flex-column gap-3">
					<div className="col-12">
						<label className="form-label fw-bold">{t("overview.projectName")}</label>
						<input
							type="text"
							className="form-control"
							value={project.name || ""}
							onChange={e => onChange({ name: e.target.value })}
						/>
					</div>

					<div className="col-12">
						<label className="form-label fw-bold">{t("common.description")}</label>
						<textarea
							className="form-control"
							value={project.description || ""}
							onChange={e => onChange({ description: e.target.value })}
							rows={3}
						/>
					</div>

					<div className="col-12">
						<label className="form-label fw-bold">{t("overview.climateRegion")}</label>
						<p className="form-control-plaintext mb-0">
							{project.climateRegion
								? `${project.climateRegion.name}`
								: t("overview.noClimateRegion")}
						</p>
					</div>

				{project.solutionId && (
						<div className="col-12">
							<label className="form-label fw-bold">{t("overview.solution")}</label>
							<p className="form-control-plaintext">
								<Link
									to={`/ui/solutions/${project.solutionId}`}
									className="text-decoration-none"
								>
									{t("overview.lastSolution")}
								</Link>
							</p>
						</div>
					)}

					<div className="col-12">
						<hr />
					</div>

					<div className="row">
						<div className="col-6">
							<label className="form-label fw-bold">{t("overview.buildings")}</label>
							<p className="form-control-plaintext mb-0">
								<span className="fs-4 text-primary">{buildingCount}</span>
								<br />
								<small className="text-muted">{t("overview.includedBuildings")}</small>
							</p>
						</div>
						<div className="col-6">
							<label className="form-label fw-bold">{t("overview.totalHeatDemand")}</label>
							<p className="form-control-plaintext mb-0">
								<span className="fs-4 text-success">
									{formatHeatDemand(totalDemand)}
								</span>
								<br />
								<small className="text-muted">{t("overview.annualDemand")}</small>
							</p>
						</div>
					</div>

					<div className="col-12">
						{isDirty ? (
							// the update works on the saved project
							<span className="text-muted" title={t("overview.saveFirst")}>
								{t("overview.updateData")}
							</span>
						) : (
							<Link
								to={`/ui/projects/${project.id}/update`}
								className="text-decoration-none">
								{t("overview.updateData")}
							</Link>
						)}
					</div>

					{buildingCount === 0 && (
						<div className="col-12">
							<div className="alert alert-info mb-0">
								<small>{t("overview.selectHint")}</small>
							</div>
						</div>
					)}
				</div>
			</div>
		</div>
	);
};
