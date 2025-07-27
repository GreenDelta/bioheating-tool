import React from "react";
import {
	Link,
	useSearchParams,
	useRouteError,
	redirect,
	useNavigate,
} from "react-router-dom";
import { Res } from "../api";

const errors = {
	redirect: redirectOf,
	navigate: navigateOf,
};
export default errors;

function redirectOf(message: string, details: string | Res<any>): Response {
	return redirect(`/ui/error?${paramsOf(message, details)}`);
}

function navigateOf(message: string, details: string | Res<any>) {
	const navigate = useNavigate();
	navigate(`/ui/error?${paramsOf(message, details)}`);
}

function paramsOf(message: string, details: string | Res<any>) {
	return new URLSearchParams({
		message,
		details: typeof details === "string" ? details : details.error,
	}).toString();
}

export const ErrorPage = () => {
	const [searchParams] = useSearchParams();
	const routeError = useRouteError();

	// Check for route error first, then fall back to search params
	let message = "An unexpected error occurred";
	let details: string | null = null;

	if (routeError) {
		if (routeError instanceof Error) {
			message = routeError.message || "Route error occurred";
			details = routeError.stack || null;
		} else if (typeof routeError === "string") {
			message = routeError;
		} else if (routeError && typeof routeError === "object") {
			// Handle response errors from loaders
			const errorObj = routeError as any;
			message =
				errorObj.statusText || errorObj.message || "Route error occurred";
			details = errorObj.status ? `Status: ${errorObj.status}` : null;
		}
	} else {
		// Fall back to search params if no route error
		message = searchParams.get("message") || message;
		details = searchParams.get("details");
	}

	return (
		<div>
			<h1>Error</h1>
			<article>
				<p>
					<strong>{message}</strong>
				</p>
				{details && (
					<details>
						<summary>Error details</summary>
						<p style={{ fontFamily: "monospace", fontSize: "0.9em" }}>
							{details}
						</p>
					</details>
				)}
				<footer>
					<Link to="/">Back to home page</Link>
				</footer>
			</article>
		</div>
	);
};
