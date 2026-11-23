import React, { useState } from "react";
import { useLoaderData } from "react-router-dom";
import { useTranslation } from "react-i18next";
import i18n, { errorText } from "../i18n";
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
	updateProject: (change: Partial<Project>) => void;

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
	const { project: loadedProject }: InputData = useLoaderData();
	const [project, setProject] = useState<Project>(loadedProject);
	const [selection, setSelection] = useState<GeoFeature[]>([]);
	const [isDirty, _setDirty] = useState(false);
	const [error, setError] = useState<string | null>(null);
	const [taskId, setTaskId] = useState<string | null>(null);

	const updateProject = (change: Partial<Project>) => {
		setProject(prev => ({ ...prev, ...change }));
		_setDirty(true);
		setError(null);
	};

	const setDirty = (b: boolean) => {
		_setDirty(b);
		setError(null);
	};

	return {
		project,
		updateProject,
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
	const { t } = useTranslation();
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
						["/", t("common.home")],
						["/ui/projects", t("projects.title")],
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
						<Map
							data={ctx.project.map}
							onSelect={ctx.setSelection}
							onChange={() => ctx.setDirty(true)}
						/>
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
		return (
			<OverviewPanel
				project={ctx.project}
				isDirty={ctx.isDirty}
				onChange={ctx.updateProject}
			/>
		);
	}
	if (selection.length > 1) {
		return (
			<MultiPanel
				projectId={ctx.project.id}
				features={selection}
				onChange={onChange}
			/>
		);
	}
	const f = selection[0];
	return isBuilding(f) ? (
		<BuildingPanel
			projectId={ctx.project.id}
			feature={f}
			map={ctx.project.map}
			onChange={onChange}
		/>
	) : (
		<StreetPanel feature={f} onChange={onChange} />
	);
};

const SaveButton = ({ ctx }: Props) => {
	const { t } = useTranslation();
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
			ctx.setError(errorText(res.error));
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
			title={ctx.isDirty ? t("editor.saveTitle") : t("editor.noChanges")}
			style={{ minWidth: "120px", whiteSpace: "nowrap" }}>
			<SaveIcon />
			{isSaving ? ` ${t("editor.saving")}` : ` ${t("editor.save")}`}
		</button>
	);
};

const CalculateButton = ({ ctx }: Props) => {
	const { t } = useTranslation();
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
			ctx.setError(errorText(res.error));
		}
		setCalculating(false);
	};

	return (
		<button
			className="btn btn-primary"
			onClick={handleCalculate}
			disabled={isCalculating}
			title={t("editor.calculateTitle")}
			style={{ minWidth: "120px", whiteSpace: "nowrap" }}>
			{isCalculating ? t("editor.calculating") : t("editor.calculate")}
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
		return i18n.t("editor.noSupplyCenter");
	}

	if (!hasIncludedHeatedBuildings) {
		return i18n.t("editor.noHeatedBuildings");
	}

	if (!hasIncludedStreets) {
		return i18n.t("editor.noStreets");
	}

	return null;
}

const ErrorPanel = ({ ctx }: Props) => {
	const { t } = useTranslation();
	if (!ctx.error) {
		return null;
	}
	return (
		<div
			className="alert alert-danger alert-dismissible fade show"
			role="alert">
			<strong>{t("common.errorOccurred")}</strong> {ctx.error}
			<button
				type="button"
				className="btn-close"
				onClick={() => ctx.setError(null)}
				aria-label={t("common.close")}></button>
		</div>
	);
};

const CalculationTaskPanel = ({ ctx }: Props) => {
	const { t } = useTranslation();
	return (
		<div>
			<div className="container">
				<div className="row">
					<div className="col">
						<BreadcrumbRow
							active={ctx.project.name}
							path={[
								["/", t("common.home")],
								["/ui/projects", t("projects.title")],
							]}
						/>

						<div className="mt-4">
							<TaskPanel
								taskId={ctx.taskId!}
								message={t("editor.calculatingMessage", { name: ctx.project.name })}
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
