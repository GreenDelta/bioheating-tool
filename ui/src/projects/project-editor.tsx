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

			<div className="container-fluid">
				<div className="row">
					<div className="col-md-8">
						<Map data={project.map} onSelect={setFeature} />
					</div>

					<div className="col-md-4">
						<FeaturePanel feature={feature} />
					</div>
				</div>
			</div>
		</div>
	);
};

const FeaturePanel = ({ feature }: { feature: GeoFeature | null }) => {
	if (!feature) {
		return <div></div>;
	}
	const [data, setData] = useState<BuildingData>(BuildingData.of(feature));
	useEffect(() => {
		setData(BuildingData.of(feature));
	}, [feature]);
	return <div className="card">
		<div className="card-body">

			<StringField label="Building" value={data.name}
				onChange={value => setData(data.copyWith({ name: value }))} />

			<NumberField label="Height (m)" value={data.height} step="0.1"
				onChange={value => setData(data.copyWith({ height: value }))} />

			<NumberField label="Storeys" value={data.storeys} step="1"
				onChange={value => setData(data.copyWith({ storeys: value }))} />

			<NumberField label="Heat demand (kWh)" value={data.heatDemand} step="0.1"
				onChange={value => setData(data.copyWith({ heatDemand: value }))} />

			<StringField label="Roof Type" value={data.roofType}
				onChange={value => setData(data.copyWith({ roofType: value }))} />

			<StringField label="Function" value={data.function}
				onChange={value => setData(data.copyWith({ function: value }))} />

			<NumberField label="Ground Area (m²)" value={data.groundArea} step="0.1"
				onChange={value => setData(data.copyWith({ groundArea: value }))} />

			<NumberField label="Heated Area (m²)" value={data.heatedArea} step="0.1"
				onChange={value => setData(data.copyWith({ heatedArea: value }))} />

			<NumberField
				label="Volume (m³)" value={data.volume} step="0.1"
				onChange={value => setData(data.copyWith({ volume: value }))} />

			<hr />
			<h6>Address Information</h6>

			<StringField label="Country" value={data.country}
				onChange={value => setData(data.copyWith({ country: value }))} />

			<StringField label="Locality/City" value={data.locality}
				onChange={value => setData(data.copyWith({ locality: value }))} />

			<StringField label="Postal Code" value={data.postalCode}
				onChange={value => setData(data.copyWith({ postalCode: value }))} />

			<StringField label="Street" value={data.street}
				onChange={value => setData(data.copyWith({ street: value }))} />

			<StringField label="Street Number" value={data.streetNumber}
				onChange={value => setData(data.copyWith({ streetNumber: value }))} />

			<NumberField label="Climate Zone" value={data.climateZone} step="1"
				onChange={value => setData(data.copyWith({ climateZone: value }))} />

			<button
				className="btn btn-primary"
				disabled={!data.isValid()}
				onClick={() => data.applyOn(feature)}>
				Update
			</button>
		</div>
	</div>
};

const StringField = ({ label, value, onChange }: {
	label: string;
	value: string;
	onChange: (value: string) => void;
}) => (
	<div className="mb-3">
		<label className="form-label">{label}</label>
		<input
			className="form-control"
			value={value}
			onChange={e => onChange(e.target.value)}
		/>
	</div>
);

const NumberField = ({ label, value, step, onChange }: {
	label: string;
	value: number;
	step?: string;
	onChange: (value: string) => void;
}) => (
	<div className="mb-3">
		<label className="form-label">{label}</label>
		<input
			type="number"
			step={step || "1"}
			className="form-control"
			value={value}
			onChange={e => onChange(e.target.value)}
		/>
	</div>
);
