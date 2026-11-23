import React from "react";
import { User } from "../model";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";

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
	const { t } = useTranslation();
	if (!user) {
		return (
			<div className="navbar-nav ms-auto">
				<Link className="nav-link" to="/ui/login">
					{t("nav.login")}
				</Link>
			</div>
		);
	}

	return (
		<>
			<div className="navbar-nav me-auto">
				<Link className="nav-link" to="/ui/projects">
					{t("nav.projects")}
				</Link>
				{user.isAdmin ? (
					<Link className="nav-link" to="/ui/users">
						{t("nav.users")}
					</Link>
				) : null}
				<Link className="nav-link" to="/ui/account">
					{t("nav.account")}
				</Link>
			</div>
			<div className="navbar-nav">
				<a
					className="nav-link"
					onClick={onLogout}
					style={{ cursor: "pointer" }}>
					{t("nav.logout")}
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
			<li className="breadcrumb-item" key={seg}>
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

export const FluidBreadcrumbRow = (props: BreadcrumbProps) => {
	return (
		<div className="row">
			<div className="col-md-12">
				<BreadcrumbRow {...props} />
			</div>
		</div>
	);
}

/// The footer at the bottom of each page.
export const Footer = () => {
	const { t } = useTranslation();
	return (
		<footer>
			<div className="container-fluid px-5">
				<nav className="navbar navbar-expand">
					<div className="navbar-nav me-auto">
						<span className="navbar-text">{t("nav.project")}</span>
					</div>
					<div className="navbar-nav">
						<Link className="nav-link" to="/ui/about">
							{t("nav.about")}
						</Link>
					</div>
				</nav>
			</div>
		</footer>
	);
};
