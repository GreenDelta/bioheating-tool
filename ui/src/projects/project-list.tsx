import React from 'react';
import { Link, useLoaderData, useNavigate } from 'react-router-dom';
import { Project } from '../model';
import { AddIcon, DeleteIcon } from '../icons';
import { Res } from '../api';

export const ProjectList = () => {

	const res: Res<Project[]> = useLoaderData();
	const projects = res.isOk ? res.value : [];

	const [deletable, setDeletable] = React.useState<Project | null>(null);

	const onDelete = (b: boolean) => {
		if (!b || !deletable) {
			setDeletable(null);
			return;
		}
		// TODO delete a project via the API
		const idx = projects.indexOf(deletable);
		if (idx > -1) {
			projects.splice(idx, 1);
		}
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

const ProjectTable = ({ projects, onDelete }: {
	projects: Project[],
	onDelete: (p: Project) => void,
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
	project: Project | null,
	doIt: (b: boolean) => void
}) => {
	if (!project) {
		return <></>;
	}
	return (
		<dialog open={true}>
			<article>
				<p>
					Do you really want to delete project <em>{project.name}</em>?
				</p>
				<footer>
					<div className="grid">
						<button className="secondary" onClick={() => doIt(false)}>Cancel</button>
						<button onClick={() => doIt(true)}>OK</button>
					</div>
				</footer>
			</article>
		</dialog>
	)
}
