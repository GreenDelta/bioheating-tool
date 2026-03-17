import React, { useState } from "react";
import { useLoaderData, useNavigate } from "react-router-dom";
import * as api from "../api";
import { ClimateRegion } from "../model";
import { BreadcrumbRow } from "../components/navi";
import { TaskPanel } from "../components/tasks";

interface FormInput {
	regions: ClimateRegion[];
}

interface FormData {
	name: string;
	description?: string;
	region: ClimateRegion;
	files: File[];
}

interface FormContext {
	regions: ClimateRegion[];
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
	const { regions }: FormInput = useLoaderData();
	const [isLoading, setLoading] = useState(false);
	const [isComplete, setComplete] = useState(false);
	const [error, setError] = useState<string | null>(null);
	const [taskId, setTaskId] = useState<string | null>(null);
	const [data, setData] = useState<FormData>({
		name: "New project",
		region: regions[0],
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
			climateRegionId: data.region.id,
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
		regions,
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
			<div className="row">
				<div className="col-md-7">
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

					<RegionCombo ctx={ctx} />

					<div className="mb-3">
						<label className="form-label">CityGML or ZIP files</label>
						<input
							type="file"
							className="form-control"
							accept=".gml,.xml,.zip"
							multiple
							onChange={e => {
								const files = e.target.files ? Array.from(e.target.files) : [];
								ctx.update({ files });
							}}
						/>
						<div className="form-text">
							Upload one or more CityGML files directly, or ZIP archives that
							contain CityGML files.
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

				<div className="col-md-5">
					<img
						src="/img/try-regions.png"
						alt="TRY Regions"
						className="img-fluid rounded shadow mb-3"
					/>
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

const RegionCombo = ({ ctx }: Props) => {
	const options = ctx.regions.map(r => (
		<option key={r.id} value={r.id}>
			{r.number}. {r.name} ({r.stationName})
		</option>
	));

	const onSelect = (sid: string) => {
		const id = parseInt(sid);
		const region = ctx.regions.find(r => r.id === id);
		if (region) {
			ctx.update({ region });
		}
	};

	return (
		<div className="mb-3">
			<label className="form-label">Climate region</label>
			<select
				className="form-select"
				value={ctx.data.region.id}
				onChange={e => onSelect(e.target.value)}>
				{options}
			</select>
		</div>
	);
};

const CreationTaskPanel = ({ ctx }: Props) => {
	return (
		<div className="container-fluid">
			<div className="row">
				<div className="col-md-8 offset-md-2">
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
