import React, { useState } from "react";
import { useLoaderData } from "react-router-dom";
import { GeoFeature, Project, isBuilding } from "../model";
import { Map } from "./map";
import { BuildingPanel } from "./panel-building";
import { StreetPanel } from "./panel-street";
import { MultiPanel } from "./panel-multi";
import { OverviewPanel } from "./panel-overview";
import { SaveIcon } from "../components/icons";
import { TaskPanel } from "../components/tasks";
import * as api from "../api";
import { BreadcrumbRow } from "../components/navi";

interface InputData {
	project: Project;
}

interface EditorContext {
	project: Project;

	selection: GeoFeature[];
	setSelection: (features: GeoFeature[]) => void;

	isDirty: boolean;
	setDirty: (b: boolean) => void;

	error: string | null;
	setError: (error: string | null) => void;

	taskId: string | null;
	setTaskId: (taskId: string | null) => void;
}

function useEditorContext(): EditorContext {
	const { project }: InputData = useLoaderData();
	const [selection, setSelection] = useState<GeoFeature[]>([]);
	const [isDirty, _setDirty] = useState(false);
	const [error, setError] = useState<string | null>(null);
	const [taskId, setTaskId] = useState<string | null>(null);

	const setDirty = (b: boolean) => {
		_setDirty(b);
		setError(null);
	};

	return {
		project,
		selection,
		setSelection,
		isDirty,
		setDirty,
		error,
		setError,
		taskId,
		setTaskId,
	};
}

export const ProjectEditor = () => {
	const ctx = useEditorContext();
	if (ctx.taskId) {
		return <CalculationTaskPanel ctx={ctx} />;
	}

	return (
		<div>
			<div className="d-flex justify-content-between align-items-center mb-3">
				<BreadcrumbRow
					active={ctx.project.name}
					path={[
						["/", "Home"],
						["/ui/projects", "Projects"],
					]}
				/>
				<div className="d-flex gap-2">
					<SaveButton ctx={ctx} />
					<CalculateButton ctx={ctx} />
				</div>
			</div>
			<ErrorPanel ctx={ctx} />
			<div className="container-fluid">
				<div className="row">
					<div className="col-md-8">
						<Map data={ctx.project.map} onSelect={ctx.setSelection} />
					</div>
					<div className="col-md-4">
						<SelectionPanel ctx={ctx} />
					</div>
				</div>
			</div>
		</div>
	);
};

interface Props {
	ctx: EditorContext;
}

/// The selection panel shows the attributes on the right side of the map.
const SelectionPanel = ({ ctx }: Props) => {
	const selection = ctx.selection;
	const onChange = () => ctx.setDirty(true);

	if (!selection || selection.length === 0) {
		return <OverviewPanel project={ctx.project} />;
	}
	if (selection.length > 1) {
		return <MultiPanel features={selection} onChange={onChange} />;
	}
	const f = selection[0];
	return isBuilding(f) ? (
		<BuildingPanel feature={f} onChange={onChange} />
	) : (
		<StreetPanel feature={f} onChange={onChange} />
	);
};

const SaveButton = ({ ctx }: Props) => {
	const [isSaving, setSaving] = useState(false);
	const handleSave = async () => {
		if (!ctx.isDirty || isSaving) {
			return;
		}
		setSaving(true);
		ctx.setError(null);
		const res = await api.updateProject(ctx.project);
		if (res.isOk) {
			ctx.setDirty(false);
		} else {
			ctx.setError(`Failed to save project: ${res.error}`);
		}
		setSaving(false);
	};

	return (
		<button
			className={
				ctx.isDirty
					? "btn btn-outline-primary me-2"
					: "btn btn-outline-secondary"
			}
			onClick={handleSave}
			disabled={!ctx.isDirty || isSaving}
			title={ctx.isDirty ? "Save changes" : "No changes to save"}
			style={{ width: "120px" }}>
			<SaveIcon />
			{isSaving ? " Saving..." : " Save"}
		</button>
	);
};

const CalculateButton = ({ ctx }: Props) => {
	const [isCalculating, setCalculating] = useState(false);
	const handleCalculate = async () => {
		if (isCalculating) return;

		// Validate project before calculation
		const validationError = validateProject(ctx.project);
		if (validationError) {
			ctx.setError(validationError);
			return;
		}

		setCalculating(true);
		ctx.setError(null);
		const res = await api.calculateSolution(ctx.project.id);
		if (res.isOk) {
			ctx.setTaskId(res.value.id);
		} else {
			ctx.setError(res.error);
		}
		setCalculating(false);
	};

	return (
		<button
			className="btn btn-primary"
			onClick={handleCalculate}
			disabled={isCalculating}
			title="Calculate solution"
			style={{ width: "120px" }}>
			{isCalculating ? " Calculating..." : " Calculate"}
		</button>
	);
};

function validateProject(project: Project): string | null {
	const features = project.map?.features || [];

	// Check for supply center
	let hasSupplyCenter = false;
	let hasIncludedHeatedBuildings = false;
	let hasIncludedStreets = false;

	for (const f of features) {
		if (isBuilding(f)) {
			const props = f.properties || {};
			if (props.isSupplyCenter) {
				hasSupplyCenter = true;
			}
			if (props.isIncluded && props.isHeated) {
				hasIncludedHeatedBuildings = true;
			}
		} else {
			// street
			const props = f.properties || {};
			if (!props.isExcluded) {
				hasIncludedStreets = true;
			}
		}
	}

	if (!hasSupplyCenter) {
		return "No supply center defined. Please set at least one building as supply hub in the network status.";
	}

	if (!hasIncludedHeatedBuildings) {
		return "No heated buildings included in the network. Please mark at least one heated building as connected.";
	}

	if (!hasIncludedStreets) {
		return "All streets are excluded. Please include at least one street in the network.";
	}

	return null;
}

const ErrorPanel = ({ ctx }: Props) => {
	if (!ctx.error) {
		return null;
	}
	return (
		<div
			className="alert alert-danger alert-dismissible fade show"
			role="alert">
			<strong>An error occured:</strong> {ctx.error}
			<button
				type="button"
				className="btn-close"
				onClick={() => ctx.setError(null)}
				aria-label="Close"></button>
		</div>
	);
};

const CalculationTaskPanel = ({ ctx }: Props) => {
	return (
		<div>
			<div className="container">
				<div className="row">
					<div className="col">
						<BreadcrumbRow
							active={ctx.project.name}
							path={[
								["/", "Home"],
								["/ui/projects", "Projects"],
							]}
						/>

						<div className="mt-4">
							<TaskPanel
								taskId={ctx.taskId!}
								message={`Calculating solution for project "${ctx.project.name}"...`}
								getTargetUrl={(result: any) =>
									result && result.id
										? `/ui/solutions/${result.id}`
										: `/ui/projects/${ctx.project.id}`
								}
							/>
						</div>
					</div>
				</div>
			</div>
		</div>
	);
};
