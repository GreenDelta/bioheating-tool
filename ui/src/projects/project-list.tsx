import React from 'react';
import { Link, useLoaderData, useNavigate } from 'react-router-dom';
import { ProjectInfo } from '../model';
import { AddIcon, DeleteIcon } from '../icons';
import * as api from '../api';

export const ProjectList = () => {

	const res: api.Res<ProjectInfo[]> = useLoaderData();
	const navigate = useNavigate();

	if (res.isErr) {
		const params = new URLSearchParams({
			message: 'Failed to load projects',
			details: res.error
		});
		navigate(`/ui/error?${params.toString()}`);
		return null;
	}

	const projects = res.value;
	const [deletable, setDeletable] = React.useState<ProjectInfo | null>(null);

	const onDelete = (b: boolean) => {
		if (!b || !deletable) {
			setDeletable(null);
			return;
		}

		const p = deletable;

		const idx = projects.indexOf(p);
		if (idx > -1) {
			projects.splice(idx, 1);
		}
		api.deleteProject(p.id).then(res => {
			if (res.isErr) {
				const params = new URLSearchParams({
					message: 'Failed to delete project',
					details: res.error
				});
				navigate(`/ui/error?${params.toString()}`);
			}
		});

		setDeletable(null);
	};

	return (
		<div>
			<h1>My Projects</h1>
			<DeleteDialog project={deletable} doIt={onDelete} />
			<ProjectTable projects={projects} onDelete={setDeletable} />
		</div>
	)
};

const ProjectTable = ({ projects, onDelete }: {	projects: ProjectInfo[],
	onDelete: (p: ProjectInfo) => void,
}) => {
	if (!projects || projects.length === 0) {
		return <div className="text-center">
			<Link to="/ui/projects/new" className="btn btn-primary">Create your first project</Link>
		</div>;
	}

	const navigate = useNavigate();

	const rows = projects.map(p => (
		<tr key={p.id}>
			<td>
				<Link to={`/ui/projects/${p.id}`} className="text-decoration-none">
					{p.name}
				</Link>
			</td>
			<td className="text-end">
				<DeleteIcon color="#dc3545" onClick={() => onDelete(p)} />
			</td>
		</tr>
	));

	return (
		<div>
			<table className="table table-hover">
				<tbody>
					{rows}
					<tr>
						<td></td>
						<td className="text-end">
							<AddIcon tooltip="Create a new project"
								onClick={() => navigate("/ui/projects/new")} />
						</td>
					</tr>
				</tbody>
			</table>
		</div>
	);
};

const DeleteDialog = ({ project, doIt }: {
	project: ProjectInfo | null,
	doIt: (b: boolean) => void
}) => {
	if (!project) {
		return <></>;
	}
	return (
		<div className="modal d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
			<div className="modal-dialog">
				<div className="modal-content">
					<div className="modal-header">
						<h5 className="modal-title">Delete project?</h5>
					</div>
					<div className="modal-body">
						<p>
							Do you really want to delete project <strong>{project.name}</strong>? Note
							that this cannot be undone.
						</p>
					</div>
					<div className="modal-footer">
						<button type="button" className="btn btn-secondary" onClick={() => doIt(false)}>
							Cancel
						</button>
						<button type="button" className="btn btn-danger" onClick={() => doIt(true)}>
							Delete
						</button>
					</div>
				</div>
			</div>
		</div>
	)
};
