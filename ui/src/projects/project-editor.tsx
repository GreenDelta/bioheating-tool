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

	return <div style={{ paddingTop: 20 }}>
		<h4>Building: {feature.properties?.name}</h4>
		<fieldset>
			<label>
				Name
				<input value={data.name}
					onChange={e => setData(data.copyWith({ name: e.target.value }))} />
			</label>

			<label>
				Height (m)
				<input type="number" step="0.1" value={data.height}
					onChange={e => setData(data.copyWith({ height: e.target.value }))} />
			</label>

			<label>
				Storeys
				<input type="number" step="1" value={data.storeys}
					onChange={e => setData(data.copyWith({ storeys: e.target.value }))} />
			</label>

			<label>
				Heat demand (kWh)
				<input
					type="number" step="0.1" value={data.heatDemand}
					onChange={e => setData(data.copyWith({ heatDemand: e.target.value }))} />
			</label>

		</fieldset>
		<button disabled={!data.isValid()}
			onClick={() => data.applyOn(feature)} style={{ marginTop: 10 }}>
			Update
		</button>
	</div>
}
