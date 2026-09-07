import React from "react";
import { useNavigation } from "react-router-dom";
import { ProgressPanel } from "./tasks";

/// Shows a centered spinner overlay whenever a route data loader is pending,
/// so that navigation to heavy pages (e.g. a project with its map) does not
/// appear to hang without any feedback.
export const NavigationLoading = () => {
	const navigation = useNavigation();
	if (navigation.state !== "loading") {
		return null;
	}
	return (
		<div
			className="d-flex align-items-center justify-content-center"
			style={{
				position: "fixed",
				top: 0,
				left: 0,
				right: 0,
				bottom: 0,
				zIndex: 2000,
				backgroundColor: "rgba(255,255,255,0.6)",
			}}>
			<ProgressPanel message="Loading data from server ..." />
		</div>
	);
};
