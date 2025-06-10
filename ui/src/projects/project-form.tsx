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
		<div>
			<h1>New project</h1>

			<ErrorRow err={error} />

			<label>
				Name
				<input type="text"
					value={data.name}
					onChange={(e) => {
						setData({ ...data, name: e.target.value });
						setError(null);
					}} />
			</label>

			<label>
				Description
				<textarea
					value={data.description}
					onChange={(e) => {
						setData({ ...data, description: e.target.value });
						setError(null);
					}}
					rows={2}
				/>
			</label>			<label>
				CityGML
				<input
					type="file"
					accept=".gml,.xml"
					onChange={(e) => {
						const file = e.target.files?.[0] || null;
						setData({...data, file});
						setError(null);
					}}
				/>
				{data.file && (
					<small style={{ color: 'var(--pico-muted-color)' }}>
						Selected: {data.file.name} ({(data.file.size / (1024**2)).toFixed(1)} MB)
					</small>
				)}
			</label>

			<div className="grid">
				<div />
				<div />
				<div className="grid">
					<button
						className="secondary"
						disabled={loading}
						onClick={() => navigate("/ui/projects")}>Cancel</button>
					<button
						disabled={loading || !isComplete(data)} onClick={handleCreate}>
						Create project
					</button>
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
		<div>
			<p style={{ textAlign: "center", color: "var(--pico-del-color)" }}>{err}</p>
		</div>
	);
};
