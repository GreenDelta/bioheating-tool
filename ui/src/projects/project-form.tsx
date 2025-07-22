import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import * as api from '../api';

interface FormData {
	name?: string;
	description?: string;
	file?: File | null;
}

function isComplete(data: FormData): boolean {
	if (!data || !data.name || !data.file)
		return false;
	var name = data.name.trim();
	return name.length > 0;
}

export const ProjectForm = () => {

	const navigate = useNavigate();
	const [data, setData] = useState<FormData>({});
	const [loading, setLoading] = useState(false);
	const [error, setError] = useState<string | null>(null);

	const handleCreate = async () => {
		if (!isComplete(data)) {
			return;
		}

		setLoading(true);
		setError(null);

		const res = await api.createProject(
			data.name!, data.description || "", data.file!);
		setLoading(false);

		if (res.isErr) {
			setError(res.error);
			return;
		}
		navigate("/ui/projects");
	};


	return (
		<div className="container-fluid">
			<div className="row">
				<div className="col-md-7">
					<h1>New project</h1>

					<ErrorRow err={error} />

					<div className="mb-3">
						<label className="form-label">Name</label>
						<input
							type="text"
							className="form-control"
							value={data.name || ''}
							onChange={(e) => {
								setData({ ...data, name: e.target.value });
								setError(null);
							}}
						/>
					</div>

					<div className="mb-3">
						<label className="form-label">Description</label>
						<textarea
							className="form-control"
							value={data.description || ''}
							onChange={(e) => {
								setData({ ...data, description: e.target.value });
								setError(null);
							}}
							rows={2}
						/>
					</div>

					<div className="mb-3">
						<label className="form-label">CityGML</label>
						<input
							type="file"
							className="form-control"
							accept=".gml,.xml"
							onChange={(e) => {
								const file = e.target.files?.[0] || null;
								setData({...data, file});
								setError(null);
							}}
						/>
						{data.file && (
							<div className="form-text">
								Selected: {data.file.name} ({(data.file.size / (1024**2)).toFixed(1)} MB)
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
							onClick={handleCreate}>
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
