import React, { useState } from 'react';
import { useLoaderData } from 'react-router-dom';
import { GeoFeature, Project, Fuel, isBuilding } from '../model';
import { Map } from './map';
import { BuildingPanel } from './panel-building';
import { StreetPanel } from './panel-street';
import { MultiPanel } from './panel-multi';
import { SaveIcon } from '../icons';
import { DownloadIcon } from '../icons';
import * as api from '../api';


interface InputData {
	project: Project;
	fuels: Fuel[];
}


interface EditorContext {

	project: Project;
	fuels: Fuel[];

	selection: GeoFeature[];
	setSelection: (features: GeoFeature[]) => void;

	isDirty: boolean;
	setDirty: (b: boolean) => void;

	error: string | null;
	setError: (error: string | null) => void;

}


function useEditorContext(): EditorContext {
	const { project, fuels }: InputData = useLoaderData();
	const [selection, setSelection] = useState<GeoFeature[]>([]);
	const [isDirty, _setDirty] = useState(false);
	const [error, setError] = useState<string | null>(null);

	const setDirty = (b: boolean) => {
		_setDirty(b);
		setError(null);
	};

	return {
		project,
		fuels,
		selection,
		setSelection,
		isDirty,
		setDirty,
		error,
		setError
	}
}


export const ProjectEditor = () => {
	const ctx = useEditorContext();
	return (
		<div>
			<div className="d-flex justify-content-between align-items-center mb-3">
				<h2>Project: {ctx.project.name}{ctx.isDirty ? "*" : ""}</h2>
				<div className="d-flex">
					<SaveButton ctx={ctx} />
					<DownloadButton ctx={ctx} />
				</div>
			</div>
			<ErrorPanel ctx={ctx} />
			<div className="container-fluid">
				<div className="row">
					<div className="col-md-8">
						<Map data={ctx.project.map} onSelect={ctx.setSelection} />
					</div>
					<div className="col-md-4">
						<SelectionPanel ctx={ctx} />
					</div>
				</div>
			</div>
		</div>
	);
};


interface Props {
	ctx: EditorContext;
}


/// The selection panel shows the attributes on the right side of the map.
const SelectionPanel = ({ ctx }: Props) => {
	const selection = ctx.selection;
	const onChange = () => ctx.setDirty(true);

	if (!selection || selection.length === 0) {
		return <div></div>;
	}
	if (selection.length > 1) {
		return <MultiPanel features={selection} onChange={onChange} />;
	}
	const f = selection[0];
	return isBuilding(f)
		? <BuildingPanel feature={f} fuels={ctx.fuels} onChange={onChange} />
		: <StreetPanel feature={f} onChange={onChange} />
}


const SaveButton = ({ ctx }: Props) => {

	const [isSaving, setSaving] = useState(false);
	const handleSave = async () => {
		if (!ctx.isDirty || isSaving) {
			return;
		}
		setSaving(true);
		ctx.setError(null);
		const res = await api.updateProject(ctx.project);
		if (res.isOk) {
			ctx.setDirty(false);
		} else {
			ctx.setError(`Failed to save project: ${res.error}`);
		}
		setSaving(false);
	};

	return (
		<button
			className={ctx.isDirty
				? "btn btn-outline-primary me-2"
				: "btn-outline-secondary"}
			onClick={handleSave}
			disabled={!ctx.isDirty || isSaving}
			title={ctx.isDirty ? 'Save changes' : 'No changes to save'}
			style={{ width: '120px' }}>
			<SaveIcon />
			{isSaving ? ' Saving...' : ' Save'}
		</button>
	);
};


const DownloadButton = ({ ctx }: Props) => {

	const [isDownloading, setDownloading] = useState(false);
	const handleDownload = async () => {
		if (isDownloading) return;
		setDownloading(true);
		ctx.setError(null);
		const res = await api.getSophenaPackage(ctx.project.id);
		if (!res.isOk) {
			ctx.setError(res.error);
		}
		setDownloading(false);
	};

	return (
		<button
			className="btn-outline-secondary"
			onClick={handleDownload}
			disabled={isDownloading}
			title="Download Sophena package"
			style={{ width: '120px' }}>
			<DownloadIcon />
			{isDownloading ? ' Downloading...' : ' Sophena'}
		</button>
	);
};


const ErrorPanel = ({ ctx }: Props) => {
	if (!ctx.error) {
		return null;
	}
	return (
		<div className="alert alert-danger alert-dismissible fade show" role="alert">
			<strong>An error occured:</strong> {ctx.error}
			<button
				type="button"
				className="btn-close"
				onClick={() => ctx.setError(null)}
				aria-label="Close"
			></button>
		</div>
	);
};
