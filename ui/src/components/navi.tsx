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
