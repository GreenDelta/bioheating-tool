import React, { useState } from 'react';
import { useLoaderData, useNavigate } from 'react-router-dom';
import * as api from '../api';
import { ClimateRegion } from '../model';

interface FormData {
	name?: string;
	description?: string;
	region: ClimateRegion;
	file?: File | null;
	error?: string | null;
}

function useFormData(regions: ClimateRegion[]) {

	const [data, setData] = useState<FormData>({
		name: "New project",
		region: regions[0],
	});

	const update = (diff: Partial<FormData>) => {
		if (diff.error) {
			setData(prev => ({ ...prev, error: diff.error }));
		} else {
			setData(prev => ({ ...prev, ...diff, error: null }));
		}
	}

	return { data, update }
}

function isComplete(data: FormData): boolean {
	if (!data || !data.name || !data.file || !data.region)
		return false;
	var name = data.name.trim();
	return name.length > 0;
}

export const ProjectForm = () => {

	const navigate = useNavigate();
	const regions: ClimateRegion[] = useLoaderData();
	const { data, update } = useFormData(regions);
	const [loading, setLoading] = useState(false);


	const onOk = async () => {
		if (!isComplete(data)) {
			return;
		}

		setLoading(true);
		const res = await api.createProject({
			climateRegionId: data.region.id,
			name: data.name!,
			file: data.file!,
			description: data.description,
		});
		setLoading(false);

		if (res.isErr) {
			update({ error: res.error });
		} else {
			navigate("/ui/projects");
		}
	};

	return (
		<div className="container-fluid">
			<div className="row">
				<div className="col-md-7">
					<h1>New project</h1>

					{data.error
						? <ErrorRow err={data.error} />
						: <></>}

					<div className="mb-3">
						<label className="form-label">Name</label>
						<input
							type="text"
							className="form-control"
							value={data.name || ''}
							onChange={(e) => update({ name: e.target.value })}
						/>
					</div>

					<div className="mb-3">
						<label className="form-label">Description</label>
						<textarea
							className="form-control"
							value={data.description || ''}
							onChange={(e) => update({ description: e.target.value })}
							rows={2}
						/>
					</div>

					<div className="mb-3">
						<label className="form-label">Climate region</label>
						<select
							className="form-select"
							value={data.region.id}
							onChange={(e) => {
								const id = parseInt(e.target.value);
								const region = regions.find(r => r.id === id);
								if (region) {
									update({ region })
								}
							}}>
							{regions.map(r => (
								<option key={r.id} value={r.id}>
									{r.number}. {r.name} ({r.stationName})
								</option>
							))}
						</select>
					</div>

					<div className="mb-3">
						<label className="form-label">CityGML</label>
						<input
							type="file"
							className="form-control"
							accept=".gml,.xml"
							onChange={(e) => {
								const file = e.target.files?.[0] || null;
								update({ file });
							}}
						/>
						{data.file && (
							<div className="form-text">
								Selected: {data.file.name} ({(data.file.size / (1024 ** 2)).toFixed(1)} MB)
							</div>
						)}
					</div>

					<div className="d-flex gap-2">
						<button
							className="btn btn-secondary"
							disabled={loading}
							onClick={() => navigate("/ui/projects")}>
							Cancel
						</button>
						<button
							className="btn btn-primary"
							disabled={loading || !isComplete(data)}
							onClick={onOk}>
							Create project
						</button>
					</div>
				</div>

				<div className="col-md-5">
					<img
						src="/img/try-regions.png"
						alt="Try Regions"
						className="img-fluid rounded shadow mb-3"
					/>
				</div>
			</div>
		</div>
	);
};

const ErrorRow = ({ err }: { err: string | null }) => {
	if (!err) {
		return <></>;
	}
	return (
		<div className="alert alert-danger" role="alert">
			{err}
		</div>
	);
};
