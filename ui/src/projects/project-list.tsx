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
		return <div>
			<Link to="/ui/projects/new">Create your first project.</Link>
		</div>;
	}

	const navigate = useNavigate();

	const rows = projects.map(p => (
		<tr>
			<td>
				<Link to={`/ui/projects/${p.id}`}>
					{p.name}
				</Link>
			</td>
			<td style={{ textAlign: "right" }}>
				<DeleteIcon color="var(--pico-del-color)" onClick={() => onDelete(p)} />
			</td>
		</tr>
	));

	return (
		<table>
			<tbody>
				{rows}
				<tr>
					<td></td>
					<td style={{ textAlign: "right" }}>
						<AddIcon tooltip="Create a new project"
							onClick={() => navigate("/ui/projects/new")} />
					</td>
				</tr>
			</tbody>
		</table>
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
		<dialog open={true}>
			<article>
				<header>
					Delete project?
				</header>
				<p>
					Do you really want to delete project <em>{project.name}</em>? Note
					that this cannot be undone.
				</p>
				<footer>
					<div className="grid">
						<div />
						<div className="grid">
							<button className="secondary" onClick={() => doIt(false)}>Cancel</button>
							<button onClick={() => doIt(true)}>OK</button>
						</div>
					</div>
				</footer>
			</article>
		</dialog>
	)
};
