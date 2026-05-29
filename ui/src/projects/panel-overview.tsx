import React from "react";
import { Link } from "react-router-dom";
import { Project, isBuilding } from "../model";
import { BuildingData } from "./panel-data";

interface Props {
	project: Project;
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

export const OverviewPanel = ({ project, onChange }: Props) => {
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
				<div className="d-flex flex-column gap-3">
					<div className="col-12">
						<label className="form-label fw-bold">Project name</label>
						<input
							type="text"
							className="form-control"
							value={project.name || ""}
							onChange={e => onChange({ name: e.target.value })}
						/>
					</div>

					<div className="col-12">
						<label className="form-label fw-bold">Description</label>
						<textarea
							className="form-control"
							value={project.description || ""}
							onChange={e => onChange({ description: e.target.value })}
							rows={3}
						/>
					</div>

					<div className="col-12">
						<label className="form-label fw-bold">Climate region</label>
						<p className="form-control-plaintext mb-0">
							{project.climateRegion
								? `${project.climateRegion.name}`
								: "No climate region assigned"}
						</p>
					</div>

				{project.solutionId && (
						<div className="col-12">
							<label className="form-label fw-bold">Solution</label>
							<p className="form-control-plaintext">
								<Link
									to={`/ui/solutions/${project.solutionId}`}
									className="text-decoration-none"
								>
									Last calculated solution
								</Link>
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
