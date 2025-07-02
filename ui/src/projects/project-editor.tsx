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
	const [isSaving, setIsSaving] = useState(false);
	const [saveError, setSaveError] = useState<string | null>(null);

	const res: api.Res<Project> = useLoaderData();
	if (res.isErr) {
		return <div style={{ color: 'red' }}>Error: {res.error}</div>;
	}
	const [project, setProject] = useState(res.value);

	const handlePanelChange = () => {
		setDirty(true);
		setSaveError(null);
	};

	const handleSave = async () => {
		if (!isDirty || isSaving) return;

		setIsSaving(true);
		setSaveError(null);

		try {
			const updateRes = await api.updateProject(project);
			if (updateRes.isOk) {
				setProject(updateRes.value);
				setDirty(false);
			} else {
				setSaveError(updateRes.error);
			}
		} catch (error) {
			setSaveError(error instanceof Error ? error.message : 'Unknown error occurred');
		} finally {
			setIsSaving(false);
		}
	};


	return (
		<div>
			<div className="d-flex justify-content-between align-items-center mb-3">
				<h2>Project: {project.name}{isDirty ? "*" : ""}</h2>
				<button
					className={`btn ${isDirty ? 'btn-primary' : 'btn-outline-secondary'}`}
					onClick={handleSave}
					disabled={!isDirty || isSaving}
					title={isDirty ? 'Save changes' : 'No changes to save'}
				>
					<SaveIcon />
					{isSaving ? ' Saving...' : ' Save'}
				</button>
			</div>

			{saveError && (
				<div className="alert alert-danger alert-dismissible fade show" role="alert">
					<strong>Save failed:</strong> {saveError}
					<button
						type="button"
						className="btn-close"
						onClick={() => setSaveError(null)}
						aria-label="Close"
					></button>
				</div>
			)}

			<div className="container-fluid">
				<div className="row">
					<div className="col-md-8">
						<Map
							data={project.map}
							onSelect={setSelection}
						/>
					</div>

					<div className="col-md-4">
						{panelOf(selection, handlePanelChange)}
					</div>
				</div>
			</div>
		</div>
	);
};

function panelOf(selection: GeoFeature[], onChange: () => void): React.JSX.Element {
	if (!selection || selection.length === 0) {
		return <div></div>;
	}
	if (selection.length > 1) {
		return <MultiPanel features={selection} onChange={onChange} />;
	}
	const f = selection[0];
	return isBuilding(f)
		? <BuildingPanel feature={f} onChange={onChange} />
		: <StreetPanel feature={f} onChange={onChange} />
}

