import React from "react";
import {
	Link,
	useSearchParams,
	useRouteError,
	redirect,
	NavigateFunction,
} from "react-router-dom";
import { useTranslation } from "react-i18next";
import type { ParseKeys } from "i18next";
import { Res } from "../api";
import i18n, { errorText } from "../i18n";

/// A translation key that describes what failed, e.g. `loadErrors.project`.
type MessageKey = ParseKeys;

const errors = {
	redirect: redirectOf,
	navigate: navigateOf,
};
export default errors;

/// Redirects to the error page; used in route loaders.
function redirectOf(message: MessageKey, details: string | Res<any>): Response {
	return redirect(`/ui/error?${paramsOf(message, details)}`);
}

/// Navigates to the error page; used in event handlers of components.
function navigateOf(
	navigate: NavigateFunction,
	message: MessageKey,
	details: string | Res<any>,
) {
	navigate(`/ui/error?${paramsOf(message, details)}`);
}

// The message key and the error code are passed as URL parameters and are
// translated when the error page is rendered.
function paramsOf(message: MessageKey, details: string | Res<any>) {
	const params = new URLSearchParams({ message });
	if (typeof details === "string") {
		params.set("details", details);
	} else {
		params.set("code", details.error);
		if (details.details) {
			params.set("details", details.details);
		}
	}
	return params.toString();
}

export const ErrorPage = () => {
	const { t } = useTranslation();
	const [searchParams] = useSearchParams();
	const routeError = useRouteError();

	// Check for route error first, then fall back to search params
	let message = t("errorPage.unexpected");
	let reason: string | null = null;
	let details: string | null = null;

	if (routeError) {
		if (routeError instanceof Error) {
			message = routeError.message || t("errorPage.routeError");
			details = routeError.stack || null;
		} else if (typeof routeError === "string") {
			message = routeError;
		} else if (routeError && typeof routeError === "object") {
			// Handle response errors from loaders
			const errorObj = routeError as any;
			message =
				errorObj.statusText || errorObj.message || t("errorPage.routeError");
			details = errorObj.status
				? t("errorPage.status", { status: errorObj.status })
				: null;
		}
	} else {
		// Fall back to search params if no route error
		const key = searchParams.get("message");
		if (key && i18n.exists(key)) {
			message = t(key as MessageKey);
		}
		const code = searchParams.get("code");
		reason = code ? errorText(code) : null;
		details = searchParams.get("details");
	}

	return (
		<div>
			<h1>{t("errorPage.title")}</h1>
			<article>
				<p>
					<strong>{message}</strong>
				</p>
				{reason && <p>{reason}</p>}
				{details && (
					<details>
						<summary>{t("errorPage.details")}</summary>
						<p style={{ fontFamily: "monospace", fontSize: "0.9em" }}>
							{details}
						</p>
					</details>
				)}
				<footer>
					<Link to="/">{t("errorPage.backHome")}</Link>
				</footer>
			</article>
		</div>
	);
};
