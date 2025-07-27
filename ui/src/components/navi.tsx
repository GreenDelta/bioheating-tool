import React from "react";
import { User } from "../model";
import { Link } from "react-router-dom";

interface MenuProps {
	user: User | null;
	onLogout: () => void;
}

export const MainMenu = ({ user, onLogout }: MenuProps) => {
	return (
		<nav className="navbar navbar-expand-lg navbar-light bg-light mb-4">
			<div className="container-fluid px-5">
				<Link className="navbar-brand" to="/">
					BIOHEATING
				</Link>
				<MainLinks {...{ user, onLogout }} />
			</div>
		</nav>
	);
};

const MainLinks = ({ user, onLogout }: MenuProps) => {
	if (!user) {
		return (
			<div className="navbar-nav ms-auto">
				<Link className="nav-link" to="/ui/login">
					Login
				</Link>
			</div>
		);
	}

	return (
		<>
			<div className="navbar-nav me-auto">
				<Link className="nav-link" to="/ui/projects">
					Projects
				</Link>
				{user.isAdmin ? (
					<Link className="nav-link" to="/ui/users">
						Users
					</Link>
				) : null}
			</div>
			<div className="navbar-nav">
				<a
					className="nav-link"
					onClick={onLogout}
					style={{ cursor: "pointer" }}>
					Logout
				</a>
			</div>
		</>
	);
};

interface BreadcrumbProps {
	active: string;
	path?: [string, string][];
}

export const BreadcrumbRow = ({ active, path }: BreadcrumbProps) => {
	const links = !path
		? null
		: path.map(([seg, label]) => (
				<li className="breadcrumb-item">
					<Link to={seg}>{label}</Link>
				</li>
			));
	return (
		<nav aria-label="breadcrumb">
			<ol className="breadcrumb">
				{links}
				<li className="breadcrumb-item active" aria-current="page">
					{active}
				</li>
			</ol>
		</nav>
	);
};
