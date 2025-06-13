import React, { useState, useEffect } from 'react';
import { Link, useLoaderData } from 'react-router-dom';
import { GeoFeature, Project } from '../model';
import { Map } from './map';
import * as api from '../api';
import { BuildingData } from './building-data';

export const ProjectEditor = () => {

	const [feature, setFeature] = useState<GeoFeature | null>(null);
	const res: api.Res<Project> = useLoaderData();
	if (res.isErr) {
		return <div style={{ color: 'red' }}>Error: {res.error}</div>;
	}
	const project = res.value;

	return (
		<div>

			<h2>Project: {project.name}</h2>

			<div>
				<Map data={project.map} onSelect={setFeature} />
			</div>

			<FeaturePanel feature={feature} />
		</div>
	);
};

const FeaturePanel = ({ feature }: { feature: GeoFeature | null }) => {
	if (!feature) {
		return <></>;
	}
	const [data, setData] = useState<BuildingData>(BuildingData.of(feature));
	useEffect(() => {
		setData(BuildingData.of(feature));
	}, [feature]);

	return <div className="mt-4">
		<h4>Building: {feature.properties?.name}</h4>
		<div className="card">
			<div className="card-body">
				<div className="mb-3">
					<label className="form-label">Name</label>
					<input
						className="form-control"
						value={data.name}
						onChange={e => setData(data.copyWith({ name: e.target.value }))}
					/>
				</div>

				<div className="mb-3">
					<label className="form-label">Height (m)</label>
					<input
						type="number"
						step="0.1"
						className="form-control"
						value={data.height}
						onChange={e => setData(data.copyWith({ height: e.target.value }))}
					/>
				</div>

				<div className="mb-3">
					<label className="form-label">Storeys</label>
					<input
						type="number"
						step="1"
						className="form-control"
						value={data.storeys}
						onChange={e => setData(data.copyWith({ storeys: e.target.value }))}
					/>
				</div>				<div className="mb-3">
					<label className="form-label">Heat demand (kWh)</label>
					<input
						type="number"
						step="0.1"
						className="form-control"
						value={data.heatDemand}
						onChange={e => setData(data.copyWith({ heatDemand: e.target.value }))}
					/>
				</div>

				<div className="mb-3">
					<label className="form-label">Roof Type</label>
					<input
						className="form-control"
						value={data.roofType}
						onChange={e => setData(data.copyWith({ roofType: e.target.value }))}
					/>
				</div>

				<div className="mb-3">
					<label className="form-label">Function</label>
					<input
						className="form-control"
						value={data.function}
						onChange={e => setData(data.copyWith({ function: e.target.value }))}
					/>
				</div>

				<div className="mb-3">
					<label className="form-label">Ground Area (m²)</label>
					<input
						type="number"
						step="0.1"
						className="form-control"
						value={data.groundArea}
						onChange={e => setData(data.copyWith({ groundArea: e.target.value }))}
					/>
				</div>

				<div className="mb-3">
					<label className="form-label">Heated Area (m²)</label>
					<input
						type="number"
						step="0.1"
						className="form-control"
						value={data.heatedArea}
						onChange={e => setData(data.copyWith({ heatedArea: e.target.value }))}
					/>
				</div>

				<div className="mb-3">
					<label className="form-label">Volume (m³)</label>
					<input
						type="number"
						step="0.1"
						className="form-control"
						value={data.volume}
						onChange={e => setData(data.copyWith({ volume: e.target.value }))}
					/>
				</div>

				<hr />
				<h6>Address Information</h6>

				<div className="mb-3">
					<label className="form-label">Country</label>
					<input
						className="form-control"
						value={data.country}
						onChange={e => setData(data.copyWith({ country: e.target.value }))}
					/>
				</div>

				<div className="mb-3">
					<label className="form-label">Locality/City</label>
					<input
						className="form-control"
						value={data.locality}
						onChange={e => setData(data.copyWith({ locality: e.target.value }))}
					/>
				</div>

				<div className="mb-3">
					<label className="form-label">Postal Code</label>
					<input
						className="form-control"
						value={data.postalCode}
						onChange={e => setData(data.copyWith({ postalCode: e.target.value }))}
					/>
				</div>

				<div className="mb-3">
					<label className="form-label">Street</label>
					<input
						className="form-control"
						value={data.street}
						onChange={e => setData(data.copyWith({ street: e.target.value }))}
					/>
				</div>

				<div className="mb-3">
					<label className="form-label">Street Number</label>
					<input
						className="form-control"
						value={data.streetNumber}
						onChange={e => setData(data.copyWith({ streetNumber: e.target.value }))}
					/>
				</div>

				<div className="mb-3">
					<label className="form-label">Climate Zone</label>
					<input
						type="number"
						step="1"
						className="form-control"
						value={data.climateZone}
						onChange={e => setData(data.copyWith({ climateZone: e.target.value }))}
					/>
				</div>

				<button
					className="btn btn-primary"
					disabled={!data.isValid()}
					onClick={() => data.applyOn(feature)}>
					Update
				</button>
			</div>
		</div>
	</div>
}
