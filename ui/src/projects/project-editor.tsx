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


class FeatureData {

	name: string;
	height: number;
	storeys: number;
	heatDemand: number;

	constructor(f: GeoFeature | FeatureData) {
		if (f instanceof FeatureData) {
			// Copy constructor
			this.name = f.name;
			this.height = f.height;
			this.storeys = f.storeys;
			this.heatDemand = f.heatDemand;
		} else {
			// Original constructor from GeoFeature
			const props = f.properties || {};
			this.name = typeof props.name === "string" ? props.name : "";
			this.height = typeof props.height === "number" ? props.height : 0;
			this.storeys = typeof props.storeys === "number" ? props.storeys : 0;
			this.heatDemand = typeof props.heatDemand === "number" ? props.heatDemand : 0;
		}
	}

	applyOn(f: GeoFeature) {
		if (!f.properties) {
			f.properties = {};
		}
		f.properties.name = this.name;
		f.properties.height = this.height;
		f.properties.storeys = this.storeys;
		f.properties.heatDemand = this.heatDemand;
	}

	isValid(): boolean {
		return (typeof this.name === "string") && this.name.trim().length > 0;
	}
}


const FeaturePanel = ({ feature }: { feature: GeoFeature | null }) => {
	if (!feature) {
		return <></>;
	}
	const [data, setData] = useState<FeatureData>(new FeatureData(feature));
	useEffect(() => {
		setData(new FeatureData(feature));
	}, [feature]);

	return <div style={{ paddingTop: 20 }}>
		<h4>Building: {feature.properties?.name}</h4>
		<fieldset>
			<label>
				Name
				<input
					value={data.name}
					onChange={e => {
						const newData = new FeatureData(data);
						newData.name = e.target.value;
						setData(newData);
					}} />
			</label>

			<label>
				Height (m)
				<input
					type="number"
					value={data.height}
					onChange={e => {
						const newData = new FeatureData(data);
						newData.height = parseFloat(e.target.value);
						setData(newData);
					}}
					step="0.1" />
			</label>

			<label>
				Storeys
				<input
					type="number"
					value={data.storeys}
					onChange={e => {
						const newData = new FeatureData(data);
						newData.storeys = parseInt(e.target.value, 10);
						setData(newData);
					}}
					step="1" />
			</label>
			<label>
				Heat demand (kWh)
				<input
					type="number"
					value={data.heatDemand}
					onChange={e => {
						const newData = new FeatureData(data);
						newData.heatDemand = parseFloat(e.target.value);
						setData(newData);
					}}
					step="0.1" />
			</label>
		</fieldset>
		<button disabled={!data.isValid()}
			onClick={() => data.applyOn(feature)} style={{ marginTop: 10 }}>
			Update
		</button>
	</div>
}
