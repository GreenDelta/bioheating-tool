import React, { useState } from 'react';
import { Link, useLoaderData } from 'react-router-dom';
import { GeoFeature, Project, isBuilding, isStreet } from '../model';
import { Map } from './map';
import { BuildingPanel } from './panel-building';
import { StreetPanel } from './panel-street';
import { MultiPanel } from './panel-multi';
import { SaveIcon } from '../icons';
import * as api from '../api';

export const ProjectEditor = () => {

	const [selection, setSelection] = useState<GeoFeature[]>([]);
	const [isDirty, setDirty] = useState(false);

	const res: api.Res<Project> = useLoaderData();
	if (res.isErr) {
		return <div style={{ color: 'red' }}>Error: {res.error}</div>;
	}
	const project = res.value;
	return (
		<div>
			<div className="d-flex justify-content-between align-items-center mb-3">
				<h2>Project: {project.name}{isDirty ? "*" : ""}</h2>
				<SaveIcon />
			</div>

			<div className="container-fluid">
				<div className="row">
					<div className="col-md-8">
						<Map
							data={project.map}
							onSelect={setSelection}
						/>
					</div>

					<div className="col-md-4">
						{panelOf(selection)}
					</div>
				</div>
			</div>
		</div>
	);
};

function panelOf(selection: GeoFeature[]): React.JSX.Element {
	if (!selection || selection.length === 0) {
		return <div></div>;
	}
	if (selection.length > 1) {
		return <MultiPanel features={selection} />;
	}
	const f = selection[0];
	return isBuilding(f)
		? <BuildingPanel feature={f} />
		: <StreetPanel feature={f} />
}

