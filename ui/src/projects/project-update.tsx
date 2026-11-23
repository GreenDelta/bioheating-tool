import React, { useState } from "react";
import { useLoaderData, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { errorText } from "../i18n";
import { Project } from "../model";
import * as api from "../api";
import { BreadcrumbRow } from "../components/navi";
import { TaskPanel } from "../components/tasks";

/// The page for updating the data of a project: importing building data from
/// an Excel file, loading new streets from OSM, and re-estimating the heat
/// demands. The steps can be combined; they are executed on the saved project
/// in a server task (see `ProjectUpdate.java`).
export const ProjectUpdatePage = () => {
	const { t } = useTranslation();
	const navigate = useNavigate();
	const { project } = useLoaderData() as { project: Project };

	const [file, setFile] = useState<File | null>(null);
	const [updateStreets, setUpdateStreets] = useState(false);
	const [estimateDemand, setEstimateDemand] = useState(false);
	const [isLoading, setLoading] = useState(false);
	const [error, setError] = useState<string | null>(null);
	const [taskId, setTaskId] = useState<string | null>(null);

	const projectUrl = `/ui/projects/${project.id}`;
	const path: [string, string][] = [
		["/", t("common.home")],
		["/ui/projects", t("projects.title")],
		[projectUrl, project.name],
	];

	if (taskId) {
		return (
			<Layout>
				<BreadcrumbRow active={t("projectUpdate.updating")} path={path} />
				<div className="mt-4">
					<TaskPanel
						taskId={taskId}
						message={t("projectUpdate.updatingMessage", { name: project.name })}
						getTargetUrl={() => projectUrl}
					/>
				</div>
			</Layout>
		);
	}

	const isComplete = !!file || updateStreets || estimateDemand;

	const onOk = async () => {
		if (!isComplete || isLoading) {
			return;
		}
		setLoading(true);
		setError(null);
		const res = await api.updateProjectData(project.id, {
			file,
			updateStreets,
			estimateDemand,
		});
		setLoading(false);
		if (res.isErr) {
			setError(errorText(res.error));
		} else {
			setTaskId(res.value.id);
		}
	};

	return (
		<Layout>
			<BreadcrumbRow active={t("projectUpdate.title")} path={path} />

			{error && (
				<div className="alert alert-danger" role="alert">
					{error}
				</div>
			)}

			<div className="mb-3">
				<label className="form-label" htmlFor="updateFile">
					{t("projectUpdate.excelFile")}
				</label>
				<input
					id="updateFile"
					type="file"
					className="form-control"
					accept=".xlsx"
					onChange={e => setFile(e.target.files?.[0] ?? null)}
				/>
			</div>

			<div className="form-check mb-2">
				<input
					id="updateStreets"
					type="checkbox"
					className="form-check-input"
					checked={updateStreets}
					onChange={e => setUpdateStreets(e.target.checked)}
				/>
				<label className="form-check-label" htmlFor="updateStreets">
					{t("projectUpdate.updateStreets")}
				</label>
			</div>

			<div className="form-check mb-3">
				<input
					id="estimateDemand"
					type="checkbox"
					className="form-check-input"
					checked={estimateDemand}
					onChange={e => setEstimateDemand(e.target.checked)}
				/>
				<label className="form-check-label" htmlFor="estimateDemand">
					{t("projectUpdate.estimateDemand")}
				</label>
			</div>

			<div className="d-flex gap-2 justify-content-end">
				<button
					className="btn btn-outline-secondary"
					disabled={isLoading}
					onClick={() => navigate(projectUrl)}
					style={{ minWidth: "120px", whiteSpace: "nowrap" }}>
					{t("common.cancel")}
				</button>
				<button
					className="btn btn-primary"
					disabled={isLoading || !isComplete}
					onClick={onOk}
					style={{ minWidth: "120px", whiteSpace: "nowrap" }}>
					{t("projectUpdate.submit")}
				</button>
			</div>
		</Layout>
	);
};

const Layout = ({ children }: { children: React.ReactNode }) => (
	<div className="container-fluid">
		<div className="row justify-content-center">
			<div className="col-12 col-md-10 col-lg-8">{children}</div>
		</div>
	</div>
);
