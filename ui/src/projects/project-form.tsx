import React, { useState } from "react";
import { useLoaderData, useNavigate } from "react-router-dom";
import * as api from "../api";
import { ClimateRegion, Fuel } from "../model";
import { BreadcrumbRow } from "../components/navi";
import { TaskPanel } from "../components/tasks";

interface FormInput {
	regions: ClimateRegion[];
	fuels: Fuel[];
}

interface FormData {
	name: string;
	description?: string;
	region: ClimateRegion;
	fuel: Fuel;
	file?: File | null;
}

interface FormContext {
	regions: ClimateRegion[];
	fuels: Fuel[];
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

function useFormContext(): FormContext {
	const navigate = useNavigate();
	const { regions, fuels }: FormInput = useLoaderData();
	const [isLoading, setLoading] = useState(false);
	const [isComplete, setComplete] = useState(false);
	const [error, setError] = useState<string | null>(null);
	const [taskId, setTaskId] = useState<string | null>(null);
	const [data, setData] = useState<FormData>({
		name: "New project",
		region: regions[0],
		fuel: fuels[0],
	});

	const update = (diff: Partial<FormData>) => {
		const next = { ...data, ...diff };
		setData(next);
		setError(null);
		setComplete(!!next.name && next.name.trim().length > 0 && !!next.file);
	};

	const onOk = async () => {
		if (!isComplete) {
			return;
		}
		setLoading(true);
		const res = await api.createProject({
			climateRegionId: data.region.id,
			fuelId: data.fuel.id,
			name: data.name!,
			file: data.file!,
			description: data.description,
		});
		setLoading(false);

		if (res.isErr) {
			setError("Failed to create project: " + res.error);
		} else {
			setTaskId(res.value.id);
		}
	};

	const onTaskSuccess = (result: any) => {
		if (result && result.id) {
			navigate(`/ui/projects/${result.id}`);
		} else {
			navigate("/ui/projects");
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
		fuels,
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

	// Show TaskPanel when task is running
	if (ctx.taskId) {
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
								taskId={ctx.taskId}
								message={`Creating project "${ctx.data.name}" and processing uploaded file...`}
								getTargetUrl={ctx.getTaskTargetUrl}
							/>
						</div>
					</div>
				</div>
			</div>
		);
	}

	// Show regular form when no task is running
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

					<FuelCombo ctx={ctx} />

					<div className="mb-3">
						<label className="form-label">CityGML</label>
						<input
							type="file"
							className="form-control"
							accept=".gml,.xml"
							onChange={e => {
								const file = e.target.files?.[0] || null;
								ctx.update({ file });
							}}
						/>
						{ctx.data.file && (
							<div className="form-text">
								Selected: {ctx.data.file.name} (
								{(ctx.data.file.size / 1024 ** 2).toFixed(1)} MB)
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

type Props = { ctx: FormContext };

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

const FuelCombo = ({ ctx }: Props) => {
	const options = ctx.fuels.map(f => (
		<option key={f.id} value={f.id}>
			{f.name} ({f.unit})
		</option>
	));

	const onSelect = (sid: string) => {
		const id = parseInt(sid);
		const fuel = ctx.fuels.find(f => f.id === id);
		if (fuel) {
			ctx.update({ fuel });
		}
	};

	return (
		<div className="mb-3">
			<label className="form-label">Default fuel</label>
			<select
				className="form-select"
				value={ctx.data.fuel.id}
				onChange={e => onSelect(e.target.value)}>
				{options}
			</select>
		</div>
	);
};
