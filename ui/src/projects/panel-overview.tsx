import React from "react";
import { Project, isBuilding, Inclusion } from "../model";
import { BuildingData } from "./panel-data";

interface Props {
	project: Project;
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
		if (data.inclusion === Inclusion.REQUIRED && data.isHeated) {
			buildingCount++;
			totalDemand += data.heatDemand;
		}
	}
	return { buildingCount, totalDemand };
}

export const OverviewPanel = ({ project }: Props) => {
	const { buildingCount, totalDemand } = keyFiguresOf(project);

	const formatHeatDemand = (value: number): string => {
		if (value >= 1000000) {
			return `${(value / 1000000).toFixed()} GWh`;
		} else if (value >= 1000) {
			return `${(value / 1000).toFixed()} MWh`;
		}
		return `${value.toFixed()} kWh`;
	};

	return (
		<div className="card">
			<div className="card-header">
				<h5 className="card-title mb-0">Project overview</h5>
			</div>
			<div className="card-body">
				<div className="row g-3">
					<div className="col-12">
						<label className="form-label fw-bold">Project name</label>
						<p className="form-control-plaintext">{project.name}</p>
					</div>

					{project.description && (
						<div className="col-12">
							<label className="form-label fw-bold">Description</label>
							<p className="form-control-plaintext">{project.description}</p>
						</div>
					)}

					{project.climateRegion && (
						<div className="col-12">
							<label className="form-label fw-bold">Climate region</label>
							<p className="form-control-plaintext">
								{project.climateRegion.number}. {project.climateRegion.name}
								<br />
								<small className="text-muted">
									Station: {project.climateRegion.stationName}
								</small>
							</p>
						</div>
					)}

					{project.defaultFuel && (
						<div className="col-12">
							<label className="form-label fw-bold">Default fuel</label>
							<p className="form-control-plaintext">
								{project.defaultFuel.name} ({project.defaultFuel.unit})
								<br />
								<small className="text-muted">
									Calorific value:{" "}
									{project.defaultFuel.calorificValue.toLocaleString()} kWh/
									{project.defaultFuel.unit}
								</small>
							</p>
						</div>
					)}

					<div className="col-12">
						<hr />
					</div>

					<div className="col-6">
						<label className="form-label fw-bold">Buildings</label>
						<p className="form-control-plaintext">
							<span className="fs-4 text-primary">{buildingCount}</span>
							<br />
							<small className="text-muted">included buildings</small>
						</p>
					</div>

					<div className="col-6">
						<label className="form-label fw-bold">Total heat demand</label>
						<p className="form-control-plaintext">
							<span className="fs-4 text-success">
								{formatHeatDemand(totalDemand)}
							</span>
							<br />
							<small className="text-muted">annual demand</small>
						</p>
					</div>

					{buildingCount === 0 && (
						<div className="col-12">
							<div className="alert alert-info mb-0">
								<small>
									Select buildings or streets on the map to view and edit their
									properties.
								</small>
							</div>
						</div>
					)}
				</div>
			</div>
		</div>
	);
};
