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
				</div>

				<div className="mb-3">
					<label className="form-label">Heat demand (kWh)</label>
					<input
						type="number"
						step="0.1"
						className="form-control"
						value={data.heatDemand}
						onChange={e => setData(data.copyWith({ heatDemand: e.target.value }))}
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
