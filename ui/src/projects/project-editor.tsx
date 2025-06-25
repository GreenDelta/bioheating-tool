import React, { useState } from 'react';
import { Link, useLoaderData } from 'react-router-dom';
import { GeoFeature, Project, isBuilding, isStreet } from '../model';
import { Map } from './map';
import { BuildingPanel } from './panel-building';
import { StreetPanel } from './panel-street';
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

			<div className="container-fluid">
				<div className="row">
					<div className="col-md-8">
						<Map data={project.map} onSelect={setFeature} />
					</div>

					<div className="col-md-4">
						{feature && isBuilding(feature) && <BuildingPanel feature={feature} />}
						{feature && isStreet(feature) && <StreetPanel feature={feature} />}
						{!feature && <div></div>}
					</div>
				</div>
			</div>
		</div>
	);
};
