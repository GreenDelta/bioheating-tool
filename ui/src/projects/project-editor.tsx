import React, { useState, useEffect } from 'react';
import { Link, useLoaderData } from 'react-router-dom';
import { GeoFeature, Project } from '../model';
import { Map } from './map';
import * as api from '../api';

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
	if (!feature.properties) {
		feature.properties = {};
	}
	const props = feature.properties;

	type Data = {
		name: string,
		height: number,
		storeys: number,
		heatDemand: number,
	}

	const [data, setData] = useState<Data>({
		name: (typeof props.name === "string" ? props.name : ""),
		height: (typeof props.height === "number" ? props.height : 0),
		storeys: (typeof props.storeys === "number" ? props.storeys : 0),
		heatDemand: (typeof props.heatDemand === "number" ? props.heatDemand : 0),
	});

	const handleUpdate = () => {
		props.name = data.name;
		props.height = data.height;
		props.storeys = data.storeys;
		props.heatDemand = data.heatDemand;
	};

	const handleChange = (field: keyof Data) => (e: React.ChangeEvent<HTMLInputElement>) => {
		const value = field === 'name' ? e.target.value : parseFloat(e.target.value) || 0;
		setData(prev => ({ ...prev, [field]: value }));
	};

	return <div style={{ paddingTop: 20 }}>
		<h4>Building: {feature.properties?.name}</h4>
		<fieldset>
			<label>
				Name
				<input
					value={data.name}
					onChange={handleChange('name')} />
			</label>

			<label>
				Height (m)
				<input
					type="number"
					value={data.height}
					onChange={handleChange('height')}
					step="0.1" />
			</label>

			<label>
				Storeys
				<input
					type="number"
					value={data.storeys}
					onChange={handleChange('storeys')}
					step="1" />
			</label>
			<label>
				Heat demand (kWh)
				<input
					type="number"
					value={data.heatDemand}
					onChange={handleChange('heatDemand')}
					step="0.1" />
			</label>
		</fieldset>
		<button onClick={handleUpdate} style={{ marginTop: 10 }}>
			Update
		</button>
	</div>
}
