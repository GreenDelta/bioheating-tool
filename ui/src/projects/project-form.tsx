import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ProjectData } from '../model';
import * as api from '../api';

export const ProjectForm = () => {

	const navigate = useNavigate();
	const [data, setData] = useState<ProjectData>({
		name: '',
		description: ''
	});
	const [loading, setLoading] = useState(false);
	const [error, setError] = useState<string | null>(null);

	const handleSubmit = async () => {
		if (!data.name.trim()) {
			setError('Project name is required');
			return;
		}

		setLoading(true);
		setError(null);
		const res = await api.createProject(data);
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
			</label>

			<label>
				CityGML
				<input type="file" />
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
						disabled={loading} onClick={handleSubmit}>
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
