import React, { useEffect } from "react";
import { createRoot } from "react-dom/client";
import {
	useNavigate,
	Outlet,
	RouterProvider,
	createBrowserRouter,
	useOutletContext,
	Navigate,
	LoaderFunctionArgs,
} from "react-router-dom";

import { User } from "./model";
import * as api from "./api";
import { LoginPage } from "./login";
import { ProjectList, ProjectForm, ProjectEditor } from "./projects";
import { UserList, UserForm } from "./users";
import { HomePage } from "./home";
import errors, { ErrorPage } from "./components/errors";
import { MainMenu } from "./components/navi";

/// The root component of the application. It contains the main menu and the
/// respective sub-components that are navigated to by routing.
const Root = () => {
	const navigate = useNavigate();
	const [user, setUser] = React.useState<User | null>(null);
	useEffect(() => {
		api.getCurrentUser().then(u => {
			if (u.isErr) {
				setUser(null);
			} else {
				setUser(u.value);
			}
		});
	}, []);

	const onLogout = () => {
		api.postLogout().then(() => setUser(null));
		navigate("/");
	};

	return (
		<>
			<MainMenu user={user} onLogout={onLogout} />
			<div className="container-fluid px-5">
				<Outlet context={[user, setUser]} />
			</div>
		</>
	);
};

const ProtectedRoutes = () => {
	const [user] = useOutletContext<[User | null, any]>();
	if (!user) {
		return <Navigate to="/ui/login" replace />;
	}
	return <Outlet context={[user]} />;
};

function main() {
	const router = createBrowserRouter([
		{
			path: "/",
			element: <Root />,
			errorElement: <ErrorPage />,
			children: [
				{
					path: "/",
					element: <HomePage />,
					index: true,
				},
				{
					path: "/ui/login",
					element: <LoginPage />,
				},
				{
					path: "/ui/error",
					element: <ErrorPage />,
				},
				{
					path: "/ui",
					element: <ProtectedRoutes />,
					children: [
						{
							path: "projects",
							Component: ProjectList,
							loader: async () => {
								const res = await api.getProjects();
								return res.isErr
									? errors.redirect("Failed to load projects", res)
									: res.value;
							},
						},
						{
							path: "projects/new",
							element: <ProjectForm />,
							loader: loadProjectFormData,
						},
						{
							path: "projects/:id",
							Component: ProjectEditor,
							loader: loadProjectData,
						},
						{
							path: "users",
							Component: UserList,
							loader: async () => {
								const res = await api.getUsers();
								return res.isErr
									? errors.redirect("Failed to load users", res)
									: res.value;
							},
						},
						{
							path: "users/new",
							element: <UserForm />,
						},
					],
				},
			],
		},
	]);

	const root = createRoot(document.getElementById("app")!);
	root.render(
		<React.StrictMode>
			<RouterProvider router={router} />
		</React.StrictMode>,
	);
}

async function loadProjectFormData() {
	const regRes = await api.getClimateRegions();
	if (regRes.isErr || regRes.value.length === 0) {
		return errors.redirect(
			"Climate regions unavailable",
			regRes.isErr ? regRes : "No climate regions returned from the server.",
		);
	}
	const fuelRes = await api.getFuels();
	if (fuelRes.isErr || fuelRes.value.length === 0) {
		return errors.redirect(
			"Fuels unavailable",
			fuelRes.isErr ? fuelRes : "No fuels returned from the server",
		);
	}
	return { regions: regRes.value, fuels: fuelRes.value };
}

async function loadProjectData({ params }: LoaderFunctionArgs) {
	const projectId = parseInt(params.id || "0", 10);
	const [projectRes, fuelsRes] = await Promise.all([
		api.getProject(projectId),
		api.getFuels(),
	]);
	if (projectRes.isErr) {
		return errors.redirect("Failed to load project", projectRes);
	}
	if (fuelsRes.isErr || fuelsRes.value.length === 0) {
		return errors.redirect(
			"Fuels unavailable",
			fuelsRes.isErr ? fuelsRes : "No fuels returned from the server",
		);
	}
	return { project: projectRes.value, fuels: fuelsRes.value };
}

main();
