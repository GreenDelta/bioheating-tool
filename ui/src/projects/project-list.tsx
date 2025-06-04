import React from 'react';
import { Link, useLoaderData, useNavigate } from 'react-router-dom';
import { Project } from '../model';
import { AddIcon, DeleteIcon } from '../icons';

export const ProjectList = () => {
	const projects: Project[] = useLoaderData();
	return (
		<div>
			<h1>My Projects</h1>
			<ProjectTable projects={projects} />
		</div>
	)
};

const ProjectTable = ({ projects }: { projects: Project[] }) => {
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
				<DeleteIcon color="var(--pico-del-color)"/>
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
}
