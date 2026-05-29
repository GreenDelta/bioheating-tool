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
import { ProjectList, ProjectForm, ProjectEditor, SolutionView } from "./projects";
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
						},
						{
							path: "projects/:id",
							Component: ProjectEditor,
							loader: loadProjectData,
						},
						{
							path: "solutions/:solutionId",
							Component: SolutionView,
							loader: loadSolution,
						},
						{
							path: "account",
							element: <UserForm />,
							loader: async () => {
								const res = await api.getCurrentUser();
								return res.isErr
									? errors.redirect("Failed to load user", res)
									: res.value;
							},
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
						{
							path: "users/:id/edit",
							element: <UserForm />,
							loader: async ({ params }) => {
								const id = parseInt(params.id!);
								const res = await api.getUser(id);
								return res.isErr
									? errors.redirect("Failed to load user", res)
									: res.value;
							},
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

async function loadProjectData({ params }: LoaderFunctionArgs) {
	const projectId = parseInt(params.id || "0", 10);
	const projectRes = await api.getProject(projectId);
	if (projectRes.isErr) {
		return errors.redirect("Failed to load project", projectRes);
	}
	return { project: projectRes.value };
}

async function loadSolution({ params }: LoaderFunctionArgs) {
	const solutionId = parseInt(params.solutionId || "0", 10);
	const solutionRes = await api.getSolution(solutionId);
	return solutionRes.isErr
		? errors.redirect("Failed to load solution", solutionRes)
		: solutionRes.value;
}

main();
