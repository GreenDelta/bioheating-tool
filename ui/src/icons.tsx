// The SVG paths of the icons in this folder were directly taken from the
// respective Google material icons: https://fonts.google.com/icons
// These icons are licensed under the Apache License, Version 2; see
// https://www.apache.org/licenses/LICENSE-2.0.html

import React from "react";

interface Props {
	tooltip?: string;
	onClick?: () => void;
	color?: string;
}

export const CalcIcon = (props: Props) => {
	return (
		<Icon
			{...props}
			path={
				<path d="M320-240h60v-80h80v-60h-80v-80h-60v80h-80v60h80v80Zm200-30h200v-60H520v60Zm0-100h200v-60H520v60Zm44-152 56-56 56 56 42-42-56-58 56-56-42-42-56 56-56-56-42 42 56 56-56 58 42 42Zm-314-70h200v-60H250v60Zm-50 472q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h560q33 0 56.5 23.5T840-760v560q0 33-23.5 56.5T760-120H200Zm0-80h560v-560H200v560Zm0-560v560-560Z" />
			}
		/>
	);
};

export const CopyIcon = (props: Props) => {
	return (
		<Icon
			{...props}
			path={
				<path d="M760-200H320q-33 0-56.5-23.5T240-280v-560q0-33 23.5-56.5T320-920h280l240 240v400q0 33-23.5 56.5T760-200ZM560-640v-200H320v560h440v-360H560ZM160-40q-33 0-56.5-23.5T80-120v-560h80v560h440v80H160Zm160-800v200-200 560-560Z" />
			}
		/>
	);
};

export const DownloadIcon = (props: Props) => {
	return (
		<Icon
			{...props}
			path={
				<path d="M480-320 280-520l56-58 104 104v-326h80v326l104-104 56 58-200 200ZM240-160q-33 0-56.5-23.5T160-240v-120h80v120h480v-120h80v120q0 33-23.5 56.5T720-160H240Z" />
			}
		/>
	);
};

export const SaveIcon = (props: Props) => {
	return (
		<Icon
			{...props}
			path={
				<path d="M840-680v480q0 33-23.5 56.5T760-120H200q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h480l160 160Zm-80 34L646-760H200v560h560v-446ZM480-240q50 0 85-35t35-85q0-50-35-85t-85-35q-50 0-85 35t-35 85q0 50 35 85t85 35ZM240-560h360v-160H240v160Zm-40-86v446-560 114Z" />
			}
		/>
	);
};

export const AddIcon = (props: Props) => {
	return (
		<Icon
			{...props}
			path={
				<path d="M440-280h80v-160h160v-80H520v-160h-80v160H280v80h160v160ZM200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h560q33 0 56.5 23.5T840-760v560q0 33-23.5 56.5T760-120H200Zm0-80h560v-560H200v560Zm0-560v560-560Z" />
			}
		/>
	);
};

export const AddComponentIcon = (props: Props) => {
	return (
		<Icon
			{...props}
			path={
				<path d="M640-121v-120H520v-80h120v-120h80v120h120v80H720v120h-80ZM120-240v-80h80v80h-80Zm160 0v-80h163q-3 21-2.5 40t3.5 40H280ZM120-400v-80h80v80h-80Zm160 0v-80h266q-23 16-41.5 36T472-400H280ZM120-560v-80h80v80h-80Zm160 0v-80h480v80H280ZM120-720v-80h80v80h-80Zm160 0v-80h480v80H280Z" />
			}
		/>
	);
};

export const ExpandMoreIcon = (props: Props) => {
	return (
		<Icon
			{...props}
			path={<path d="M480-345 240-585l56-56 184 184 184-184 56 56-240 240Z" />}
		/>
	);
};

export const ExpandLessIcon = (props: Props) => {
	return (
		<Icon
			{...props}
			path={<path d="M504-480 320-664l56-56 240 240-240 240-56-56 184-184Z" />}
		/>
	);
};

export const DeleteIcon = (props: Props) => {
	return (
		<Icon
			{...props}
			path={
				<path d="M280-120q-33 0-56.5-23.5T200-200v-520h-40v-80h200v-40h240v40h200v80h-40v520q0 33-23.5 56.5T680-120H280Zm400-600H280v520h400v-520ZM360-280h80v-360h-80v360Zm160 0h80v-360h-80v360ZM280-720v520-520Z" />
			}
		/>
	);
};

const Icon = ({
	tooltip,
	onClick,
	path,
	color,
}: {
	tooltip?: string;
	onClick?: () => void;
	color?: string;
	path: React.JSX.Element;
}) => {
	const style: React.CSSProperties = {
		color: color || "var(--bs-primary)",
	};
	if (onClick) {
		style.cursor = "pointer";
	}
	return (
		<svg
			xmlns="http://www.w3.org/2000/svg"
			height="24"
			viewBox="0 -960 960 960"
			width="24"
			onClick={onClick}
			style={style}>
			<title>{tooltip}</title>
			{path}
		</svg>
	);
};
