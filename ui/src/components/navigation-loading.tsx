import React from "react";
import { useNavigation } from "react-router-dom";

const barCss = `
@keyframes bh-loading-stripes {
  0%   { background-position: 0 0; }
  100% { background-position: 1rem 0; }
}
.bh-navigation-bar {
  background-color: #0d6efd;
  background-image: repeating-linear-gradient(
    45deg,
    rgba(255,255,255,.2) 0 0.5rem,
    transparent 0.5rem 1rem
  );
  animation: bh-loading-stripes 0.3s linear infinite;
}
`;

/// Shows a thin, non-intrusive loading bar at the top of the page whenever a
/// route data loader is pending, so that navigation to heavy pages (e.g. a
/// project with its map) does not appear to hang without any feedback.
export const NavigationLoading = () => {
	const navigation = useNavigation();
	if (navigation.state !== "loading") {
		return null;
	}
	return (
		<>
			<style>{barCss}</style>
			<div
				className="bh-navigation-bar"
				role="progressbar"
				aria-label="Loading"
				style={{
					position: "fixed",
					top: 0,
					left: 0,
					right: 0,
					height: 4,
					zIndex: 2000,
				}} />
		</>
	);
};
