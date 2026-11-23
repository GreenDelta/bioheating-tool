import React, { useState } from "react";
import { useLoaderData, Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Solution, round } from "../model";
import { errorText, formatDateTime, formatNumber } from "../i18n";
import { DownloadIcon } from "../components/icons";
import { BreadcrumbRow } from "../components/navi";
import * as api from "../api";


export function SolutionView() {
	const { t } = useTranslation();
	const solution: Solution = useLoaderData();
	const [error, setError] = useState<string | null>(null);
	const [isDownloading, setDownloading] = useState(false);
	const [isDownloadingXls, setDownloadingXls] = useState(false);

	const handleDownload = async () => {
		if (isDownloading) return;
		setDownloading(true);
		setError(null);
		const res = await api.getSophenaPackage(solution.id);
		if (!res.isOk) {
			setError(errorText(res.error));
		}
		setDownloading(false);
	};

	const handleDownloadXls = async () => {
		if (isDownloadingXls) return;
		setDownloadingXls(true);
		setError(null);
		const res = await api.getSolutionXls(solution.id);
		if (!res.isOk) {
			setError(errorText(res.error));
		}
		setDownloadingXls(false);
	};

	return (
		<div>
			<div className="d-flex justify-content-between align-items-center mb-3">
				<BreadcrumbRow
					active={t("solution.title")}
					path={[
						["/ui/projects", t("projects.title")],
						[`/ui/projects/${solution.projectId}`, solution.name]
					]}
				/>
				<div className="d-flex gap-2">
					<button
						className="btn btn-outline-secondary"
						onClick={handleDownloadXls}
						disabled={isDownloadingXls}
						title={t("solution.excelTitle")}
						style={{ width: "140px" }}>
						<DownloadIcon />
						{isDownloadingXls ? ` ${t("common.downloading")}` : " Excel"}
					</button>
					<button
						className="btn btn-outline-secondary"
						onClick={handleDownload}
						disabled={isDownloading}
						title={t("solution.sophenaTitle")}
						style={{ width: "140px" }}>
						<DownloadIcon />
						{isDownloading ? ` ${t("common.downloading")}` : " Sophena"}
					</button>
				</div>
			</div>

			{error && (
				<div className="alert alert-danger alert-dismissible fade show" role="alert">
					<strong>{t("common.errorOccurred")}</strong> {error}
					<button
						type="button"
						className="btn-close"
						onClick={() => setError(null)}
						aria-label={t("common.close")}></button>
				</div>
			)}

			<div className="container-fluid">
				<div className="row">
					<div className="col-md-8">
						<ImagePanel solution={solution} />
					</div>
					<div className="col-md-4">
						<DetailsPanel solution={solution} />
					</div>
				</div>
			</div>
		</div>
	);
}

const ImagePanel = ({ solution }: { solution: Solution }) => {
	const { t } = useTranslation();
	const [hasImage, setHasImage] = useState(true);
	return (
		<div className="card h-100">
			<div className="card-header">
				<h5 className="card-title mb-0">{t("solution.network")}</h5>
			</div>
			<div className="card-body d-flex align-items-center justify-content-center">
				{hasImage ? (
					<img
						src={api.getSolutionImageUrl(solution.id)}
						alt={t("solution.imageAlt", { name: solution.name })}
						className="img-fluid border rounded"
						style={{ maxWidth: "100%", maxHeight: "70vh", objectFit: "contain" }}
						onError={() => setHasImage(false)}
					/>
				) : (
					<div className="text-muted p-4 text-center border rounded">
						{t("solution.noImage")}
					</div>
				)}
			</div>
		</div>
	);
};

const DetailsPanel = ({ solution }: { solution: Solution }) => {
	const { t } = useTranslation();
	return (
		<div className="card">
			<div className="card-header">
				<h5 className="card-title mb-0">{t("solution.details")}</h5>
			</div>
			<div className="card-body">
				<div className="row g-3">
					<div className="col-12">
						<label className="form-label fw-bold">{t("solution.project")}</label>
						<p className="form-control-plaintext">
							<Link
								to={`/ui/projects/${solution.projectId}`}
								className="text-decoration-none">
								{solution.name}
							</Link>
						</p>
					</div>

					{solution.calculatedAt && (
						<div className="col-12">
							<label className="form-label fw-bold">{t("solution.calculatedAt")}</label>
							<p className="form-control-plaintext">
								{formatDateTime(solution.calculatedAt)}
							</p>
						</div>
					)}

					{solution.heatDemand != null && (
						<div className="col-12">
							<label className="form-label fw-bold">{t("solution.heatDemand")}</label>
							<p className="form-control-plaintext">
								{formatNumber(round(solution.heatDemand / 1000, 2))} MWh
							</p>
						</div>
					)}

					{solution.length != null && (
						<div className="col-12">
							<label className="form-label fw-bold">{t("solution.networkLength")}</label>
							<p className="form-control-plaintext">
								{formatNumber(round(solution.length, 2))} m
							</p>
						</div>
					)}

					{solution.heatDemand != null && solution.length != null && solution.length > 0 && (
						<div className="col-12">
							<label className="form-label fw-bold">{t("solution.linearHeatDensity")}</label>
							<p className="form-control-plaintext">
								{formatNumber(round(solution.heatDemand / (1000 * solution.length), 2))} MWh/(m·a)
							</p>
						</div>
					)}
				</div>
			</div>
		</div>
	);
};
