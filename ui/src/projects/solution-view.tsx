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

	const handleDownload = async () => {
		if (isDownloading) return;
		setDownloading(true);
		setError(null);
		const res = await api.getSophenaPackage(solution.projectId);
		if (!res.isOk) {
			setError(res.error);
		}
		setDownloading(false);
	};

	return (
		<div className="container">
			<BreadcrumbRow
				active={`Solution`}
				path={[
					["/ui/projects", "Projects"],
					[`/ui/projects/${solution.projectId}`, solution.name]
				]}
			/>

			<div className="row mb-4">
				<div className="col">
					<div className="card">
						<div className="card-header d-flex justify-content-between align-items-center">
							<h5 className="card-title mb-0">Solution Details</h5>
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
						<div className="card-body">
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

							<div className="mb-3">
								<strong>Solution for project:</strong> {solution.name}
							</div>

							{solution.calculatedAt && (
								<div className="mb-3">
									<strong>Calculated At:</strong> {solution.calculatedAt}
								</div>
							)}

							<div className="mb-3">
								<strong>Solution Image:</strong>
								<div className="mt-2">
									<img
										src={api.getSolutionImageUrl(solution.id)}
										alt={`Solution visualization for ${solution.name}`}
										className="img-fluid border rounded"
										style={{ maxWidth: "100%", maxHeight: "600px" }}
										onError={(e) => {
											const target = e.target as HTMLImageElement;
											target.style.display = "none";
											const parent = target.parentElement;
											if (parent) {
												parent.innerHTML = '<div class="text-muted p-3 border rounded">No image available</div>';
											}
										}}
									/>
								</div>
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>
	);
}
