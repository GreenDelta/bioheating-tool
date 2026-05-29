import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import * as api from "../api";
import { BreadcrumbRow } from "../components/navi";
import { TaskPanel } from "../components/tasks";

interface FormData {
	name: string;
	description?: string;
	files: File[];
}

interface FormContext {
	data: FormData;
	error: string | null;
	taskId: string | null;
	update: (diff: Partial<FormData>) => void;
	isComplete: boolean;
	isLoading: boolean;
	onOk: () => void;
	onCancel: () => void;
	getTaskTargetUrl: (result: any) => string;
}

type Props = { ctx: FormContext };

function useFormContext(): FormContext {
	const navigate = useNavigate();
	const [isLoading, setLoading] = useState(false);
	const [isComplete, setComplete] = useState(false);
	const [error, setError] = useState<string | null>(null);
	const [taskId, setTaskId] = useState<string | null>(null);
	const [data, setData] = useState<FormData>({
		name: "New project",
		files: [],
	});

	const update = (diff: Partial<FormData>) => {
		const next = { ...data, ...diff };
		setData(next);
		setError(null);
		setComplete(
			!!next.name &&
			next.name.trim().length > 0 &&
			next.files &&
			next.files.length > 0,
		);
	};

	const onOk = async () => {
		if (!isComplete) {
			return;
		}
		setLoading(true);
		const res = await api.createProject({
			name: data.name!,
			files: data.files,
			description: data.description,
		});
		setLoading(false);

		if (res.isErr) {
			setError("Failed to create project: " + res.error);
		} else {
			setTaskId(res.value.id);
		}
	};

	const getTaskTargetUrl = (result: any) => {
		if (result && result.id) {
			return `/ui/projects/${result.id}`;
		} else {
			return "/ui/projects";
		}
	};

	const onCancel = () => {
		navigate("/ui/projects");
	};

	return {
		data,
		error,
		taskId,
		update,
		isComplete,
		isLoading,
		onOk,
		onCancel,
		getTaskTargetUrl,
	};
}

export const ProjectForm = () => {
	const ctx = useFormContext();
	if (ctx.taskId) {
		return <CreationTaskPanel ctx={ctx} />;
	}

	return (
		<div className="container-fluid">
			<div className="row justify-content-center">
				<div className="col-12 col-md-10 col-lg-8">
					<BreadcrumbRow
						active="New"
						path={[
							["/", "Home"],
							["/ui/projects", "Projects"],
						]}
					/>

					<ErrorRow ctx={ctx} />

					<div className="mb-3">
						<label className="form-label">Name</label>
						<input
							type="text"
							className="form-control"
							value={ctx.data.name || ""}
							onChange={e => ctx.update({ name: e.target.value })}
						/>
					</div>

					<div className="mb-3">
						<label className="form-label">Description</label>
						<textarea
							className="form-control"
							value={ctx.data.description || ""}
							onChange={e => ctx.update({ description: e.target.value })}
							rows={2}
						/>
					</div>

					<div className="mb-3">
						<label className="form-label">CityGML, Excel, or ZIP files</label>
						<input
							type="file"
							className="form-control"
							accept=".gml,.xml,.xlsx,.zip"
							multiple
							onChange={e => {
								const files = e.target.files ? Array.from(e.target.files) : [];
								ctx.update({ files });
							}}
						/>
						<div className="form-text">
							Upload one or more CityGML or Excel files directly, or ZIP archives that
							contain any mix of those files.
						</div>
						{ctx.data.files && ctx.data.files.length > 0 && (
							<div className="form-text">
								{ctx.data.files.length} file(s) selected (
								{(
									ctx.data.files.reduce((sum, f) => sum + f.size, 0) /
									1024 ** 2
								).toFixed(1)}{" "}
								MB)
								<ul className="mt-1 mb-0 ps-3">
									{ctx.data.files.map((f, i) => (
										<li key={i}>{f.name}</li>
									))}
								</ul>
							</div>
						)}
					</div>

					<div className="d-flex gap-2 justify-content-end">
						<button
							className="btn btn-outline-secondary"
							disabled={ctx.isLoading}
							onClick={ctx.onCancel}
							style={{ width: "120px" }}>
							Cancel
						</button>
						<button
							className="btn btn-outline-primary"
							disabled={ctx.isLoading || !ctx.isComplete}
							onClick={ctx.onOk}
							style={{ width: "120px" }}>
							OK
						</button>
					</div>
				</div>
			</div>
		</div>
	);
};

const ErrorRow = ({ ctx }: Props) => {
	if (!ctx.error) {
		return <></>;
	}
	return (
		<div className="alert alert-danger" role="alert">
			{ctx.error}
		</div>
	);
};

const CreationTaskPanel = ({ ctx }: Props) => {
	return (
		<div className="container-fluid">
			<div className="row justify-content-center">
				<div className="col-12 col-md-10 col-lg-8">
					<BreadcrumbRow
						active="Creating..."
						path={[
							["/", "Home"],
							["/ui/projects", "Projects"],
							["/ui/projects/new", "New"],
						]}
					/>

					<div className="mt-4">
						<TaskPanel
							taskId={ctx.taskId!}
							message={`Creating project "${ctx.data.name}" and processing uploaded files...`}
							getTargetUrl={ctx.getTaskTargetUrl}
						/>
					</div>
				</div>
			</div>
		</div>
	);
};
