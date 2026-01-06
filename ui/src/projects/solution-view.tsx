import React, { useState } from "react";
import { useLoaderData, Link } from "react-router-dom";
import { Solution } from "../model";
import { DownloadIcon } from "../components/icons";
import { BreadcrumbRow } from "../components/navi";
import * as api from "../api";


export function SolutionView() {
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
			setError(res.error);
		}
		setDownloading(false);
	};

	const handleDownloadXls = async () => {
		if (isDownloadingXls) return;
		setDownloadingXls(true);
		setError(null);
		const res = await api.getSolutionXls(solution.id);
		if (!res.isOk) {
			setError(res.error);
		}
		setDownloadingXls(false);
	};

	return (
		<div>
			<div className="d-flex justify-content-between align-items-center mb-3">
				<BreadcrumbRow
					active={`Solution`}
					path={[
						["/ui/projects", "Projects"],
						[`/ui/projects/${solution.projectId}`, solution.name]
					]}
				/>
				<div className="d-flex gap-2">
					<button
						className="btn btn-outline-secondary"
						onClick={handleDownloadXls}
						disabled={isDownloadingXls}
						title="Download pipe plan as Excel file"
						style={{ width: "140px" }}>
						<DownloadIcon />
						{isDownloadingXls ? " Downloading..." : " Excel"}
					</button>
					<button
						className="btn btn-outline-secondary"
						onClick={handleDownload}
						disabled={isDownloading}
						title="Download Sophena package"
						style={{ width: "140px" }}>
						<DownloadIcon />
						{isDownloading ? " Downloading..." : " Sophena"}
					</button>
				</div>
			</div>

			{error && (
				<div className="alert alert-danger alert-dismissible fade show" role="alert">
					<strong>An error occurred:</strong> {error}
					<button
						type="button"
						className="btn-close"
						onClick={() => setError(null)}
						aria-label="Close"></button>
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
	return (
		<div className="card h-100">
			<div className="card-header">
				<h5 className="card-title mb-0">Network solution</h5>
			</div>
			<div className="card-body d-flex align-items-center justify-content-center">
				<img
					src={api.getSolutionImageUrl(solution.id)}
					alt={`Solution visualization for ${solution.name}`}
					className="img-fluid border rounded"
					style={{ maxWidth: "100%", maxHeight: "70vh", objectFit: "contain" }}
					onError={(e) => {
						const target = e.target as HTMLImageElement;
						target.style.display = "none";
						const parent = target.parentElement;
						if (parent) {
							parent.innerHTML = '<div class="text-muted p-4 text-center border rounded">No image available</div>';
						}
					}}
				/>
			</div>
		</div>
	);
};

const DetailsPanel = ({ solution }: { solution: Solution }) => {
	return (
		<div className="card">
			<div className="card-header">
				<h5 className="card-title mb-0">Details</h5>
			</div>
			<div className="card-body">
				<div className="row g-3">
					<div className="col-12">
						<label className="form-label fw-bold">Project</label>
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
							<label className="form-label fw-bold">Calculated at</label>
							<p className="form-control-plaintext">
								{new Date(solution.calculatedAt).toLocaleString()}
							</p>
						</div>
					)}
				</div>
			</div>
		</div>
	);
};
