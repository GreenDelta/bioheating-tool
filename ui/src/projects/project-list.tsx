import React from "react";
import {
	Link,
	NavigateFunction,
	useLoaderData,
	useNavigate,
} from "react-router-dom";
import { Trans, useTranslation } from "react-i18next";
import { ProjectInfo } from "../model";
import { AddIcon, DeleteIcon, EditIcon } from "../components/icons";
import * as api from "../api";
import { FluidBreadcrumbRow } from "../components/navi";
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
				errors.navigate(navigate, "loadErrors.deleteProject", res);
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
	const { t } = useTranslation();
	const ctx = useProjectListContext();
	const content = !ctx.projects || ctx.projects.length === 0
		? <NoContentPanel ctx={ctx} />
		: <ProjectTable ctx={ctx} />;

	return (
		<div>
			<div className="container-fluid">
				<FluidBreadcrumbRow
					active={t("projects.title")}
					path={[["/", t("common.home")]]}
				/>
				<div className="row justify-content-center">
					<div className="col-12 col-md-10 col-lg-8">
						{content}
					</div>
				</div>
			</div>
			<DeleteDialog ctx={ctx} />
		</div>
	);
};

type Props = { ctx: ProjectListContext };

const NoContentPanel = ({ ctx }: Props) => {
	const { t } = useTranslation();
	return (
		<>
			<div className="text-center my-3">
				{t("projects.noProjects")}
				<div className="my-3">
					<Link to="/ui/projects/new" className="btn btn-outline-primary">
						{t("projects.createFirst")}
					</Link>
				</div>
			</div>
		</>
	);
};

const ProjectTable = ({ ctx }: Props) => {
	const { t } = useTranslation();
	const rows = ctx.projects.map(p => (
		<tr key={p.id}>
			<td>
				<Link to={`/ui/projects/${p.id}`} className="text-decoration-none">
					{p.name}
				</Link>
			</td>
			<td className="text-end">
				<div className="d-flex gap-2 justify-content-end">
					<EditIcon
						onClick={() => ctx.navigate(`/ui/projects/${p.id}`)}
						tooltip={t("projects.edit")} />
					<DeleteIcon
						color="#dc3545"
						onClick={() => ctx.setDeletable(p)}
						tooltip={t("projects.delete")} />
				</div>
			</td>
		</tr>
	));

	return (
		<div>
			<table className="table table-hover">
				<thead>
					<tr>
						<th>{t("projects.project")}</th>
						<th className="text-end">{t("common.actions")}</th>
					</tr>
				</thead>
				<tbody>
					{rows}
					<tr>
						<td></td>
						<td className="text-end">
							<AddIcon
								tooltip={t("projects.createNew")}
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
	const { t } = useTranslation();
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
						<h5 className="modal-title">{t("projects.deleteTitle")}</h5>
					</div>
					<div className="modal-body">
						<p>
							<Trans
								i18nKey="projects.deleteConfirm"
								values={{ name: ctx.deletable.name }}
								components={{ 1: <strong /> }}
							/>
						</p>
					</div>
					<div className="modal-footer">
						<button
							type="button"
							className="btn btn-secondary"
							onClick={() => ctx.onDelete(false)}>
							{t("common.cancel")}
						</button>
						<button
							type="button"
							className="btn btn-danger"
							onClick={() => ctx.onDelete(true)}>
							{t("common.delete")}
						</button>
					</div>
				</div>
			</div>
		</div>
	);
};
