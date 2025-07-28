import React from "react";
import {
	Link,
	NavigateFunction,
	useLoaderData,
	useNavigate,
} from "react-router-dom";
import { ProjectInfo } from "../model";
import { AddIcon, DeleteIcon } from "../components/icons";
import * as api from "../api";
import { BreadcrumbRow } from "../components/navi";
import errors from "../components/errors";

interface ProjectListContext {
	navigate: NavigateFunction;
	projects: ProjectInfo[];
	deletable: ProjectInfo | null;
	setDeletable: (p: ProjectInfo) => void;
	onDelete: (b: boolean) => void;
}

function useProjectListContext(): ProjectListContext {
	const navigate = useNavigate();
	const projects: ProjectInfo[] = useLoaderData();
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
		setDeletable(null);
		api.deleteProject(p.id).then(res => {
			if (res.isErr) {
				errors.navigate("Failed to delete project", res);
			}
		});
	};

	return {
		navigate,
		projects,
		deletable,
		setDeletable,
		onDelete,
	};
}

export const ProjectList = () => {
	const ctx = useProjectListContext();

	const content =
		!ctx.projects || ctx.projects.length === 0 ? (
			<NoContentPanel ctx={ctx} />
		) : (
			<>
				<DeleteDialog ctx={ctx} />
				<ProjectTable ctx={ctx} />
			</>
		);

	return (
		<div>
			<BreadcrumbRow active="Projects" path={[["/", "Home"]]} />
			{content}
		</div>
	);
};

type Props = { ctx: ProjectListContext };

const NoContentPanel = ({ ctx }: Props) => {
	return (
		<>
			<div className="text-center my-3">
				You do not have any created projects yet.
				<div className="my-3">
					<Link to="/ui/projects/new" className="btn btn-outline-primary">
						Create your first project
					</Link>
				</div>
			</div>
		</>
	);
};

const ProjectTable = ({ ctx }: Props) => {
	const rows = ctx.projects.map(p => (
		<tr key={p.id}>
			<td>
				<Link to={`/ui/projects/${p.id}`} className="text-decoration-none">
					{p.name}
				</Link>
			</td>
			<td className="text-end">
				<DeleteIcon color="#dc3545" onClick={() => ctx.setDeletable(p)} />
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
							<AddIcon
								tooltip="Create a new project"
								onClick={() => ctx.navigate("/ui/projects/new")}
							/>
						</td>
					</tr>
				</tbody>
			</table>
		</div>
	);
};

const DeleteDialog = ({ ctx }: Props) => {
	if (!ctx.deletable) {
		return <></>;
	}
	return (
		<div
			className="modal d-block"
			style={{ backgroundColor: "rgba(0,0,0,0.5)" }}>
			<div className="modal-dialog">
				<div className="modal-content">
					<div className="modal-header">
						<h5 className="modal-title">Delete project?</h5>
					</div>
					<div className="modal-body">
						<p>
							Do you really want to delete project{" "}
							<strong>{ctx.deletable.name}</strong>? Note that this cannot be
							undone.
						</p>
					</div>
					<div className="modal-footer">
						<button
							type="button"
							className="btn btn-secondary"
							onClick={() => ctx.onDelete(false)}>
							Cancel
						</button>
						<button
							type="button"
							className="btn btn-danger"
							onClick={() => ctx.onDelete(true)}>
							Delete
						</button>
					</div>
				</div>
			</div>
		</div>
	);
};
