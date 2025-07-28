import React from "react";
import { Project, Fuel, isBuilding } from "../model";

interface Props {
	project: Project;
	fuels: Fuel[];
}

export const OverviewPanel = ({ project, fuels }: Props) => {
	// Calculate building statistics
	const buildings = project.map.features.filter(isBuilding);
	const buildingCount = buildings.length;

	// Calculate total heat demand
	const totalHeatDemand = buildings.reduce((total, building) => {
		const heatDemand = building.properties?.heatDemand || 0;
		return total + (typeof heatDemand === 'number' ? heatDemand : 0);
	}, 0);

	// Format heat demand with appropriate units
	const formatHeatDemand = (value: number): string => {
		if (value >= 1000000) {
			return `${(value / 1000000).toFixed(1)} GWh`;
		} else if (value >= 1000) {
			return `${(value / 1000).toFixed(1)} MWh`;
		}
		return `${value.toFixed(1)} kWh`;
	};

	return (
		<div className="card">
			<div className="card-header">
				<h5 className="card-title mb-0">Project Overview</h5>
			</div>
			<div className="card-body">
				<div className="row g-3">
					<div className="col-12">
						<label className="form-label fw-bold">Project Name</label>
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
							<label className="form-label fw-bold">Climate Region</label>
							<p className="form-control-plaintext">
								{project.climateRegion.number}. {project.climateRegion.name}
								<br />
								<small className="text-muted">
									Station: {project.climateRegion.stationName} ({project.climateRegion.stationId})
								</small>
							</p>
						</div>
					)}

					{project.defaultFuel && (
						<div className="col-12">
							<label className="form-label fw-bold">Default Fuel</label>
							<p className="form-control-plaintext">
								{project.defaultFuel.name} ({project.defaultFuel.unit})
								<br />
								<small className="text-muted">
									Calorific value: {project.defaultFuel.calorificValue.toLocaleString()} kWh/{project.defaultFuel.unit}
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
							<span className="fs-4 text-primary">{buildingCount.toLocaleString()}</span>
							<br />
							<small className="text-muted">included buildings</small>
						</p>
					</div>

					<div className="col-6">
						<label className="form-label fw-bold">Total Heat Demand</label>
						<p className="form-control-plaintext">
							<span className="fs-4 text-success">{formatHeatDemand(totalHeatDemand)}</span>
							<br />
							<small className="text-muted">annual demand</small>
						</p>
					</div>

					{buildingCount > 0 && (
						<div className="col-12">
							<div className="alert alert-info mb-0">
								<small>
									<strong>Tip:</strong> Select buildings or streets on the map to view and edit their properties.
								</small>
							</div>
						</div>
					)}
				</div>
			</div>
		</div>
	);
};
