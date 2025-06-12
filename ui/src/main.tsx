import React, { useEffect } from 'react';
import { createRoot } from 'react-dom/client';
import {
	useNavigate,
	Outlet,
	Link,
	RouterProvider,
	createBrowserRouter,
	useParams,
	useOutletContext,
	Navigate
} from 'react-router-dom';

import { User } from './model';
import * as api from './api';
import { LoginPage } from './login';
import { ProjectList, ProjectForm, ProjectEditor } from './projects';
import { HomePage } from './home';
import { ErrorPage } from './error-page';


const MainMenu = (props: { user: User | null, onLogout: () => void }) => {

	let content: React.JSX.Element;
	if (!props.user) {
		content = <>
			<div className="navbar-nav ms-auto">
				<Link className="nav-link" to="/ui/login">Login</Link>
			</div>
		</>;
	} else {
		content = <>
			<div className="navbar-nav me-auto">
				<Link className="nav-link" to="/ui/projects">Projects</Link>
				<Link className="nav-link" to="/ui/users">Users</Link>
			</div>
			<div className="navbar-nav">
				<a className="nav-link" onClick={props.onLogout} style={{ cursor: 'pointer' }}>Logout</a>
			</div>
		</>;
	}

	return (
		<nav className="navbar navbar-expand-lg navbar-light bg-light mb-4">
			<div className="container-fluid px-5">
				<Link className="navbar-brand" to="/">BIOHEATING</Link>
				{content}
			</div>
		</nav>
	);
};

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
	}, [])

	const onLogout = () => {
		api.postLogout().then(() => setUser(null));
		navigate("/");
	};

	return <>
		<MainMenu user={user} onLogout={onLogout} />
		<div className="container-fluid px-5">
			<Outlet context={[user, setUser]} />
		</div>
	</>;
}


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
					element: <LoginPage />
				},
				{
					path: "/ui/error",
					element: <ErrorPage />
				},
				{
					path: "/ui",
					element: <ProtectedRoutes />,
					children: [
						{
							path: "projects",
							Component: ProjectList,
							loader: api.getProjects,
						},
						{
							path: "projects/new",
							element: <ProjectForm />
						},
						{
							path: "projects/:id",
							Component: ProjectEditor,
							loader: async ({ params }) => {
								return api.getProject(parseInt(params.id || '0', 10));
							}
						}
					]
				}
			]
		}
	]);

	const root = createRoot(document.getElementById('app')!);
	root.render(
		<React.StrictMode>
			<RouterProvider router={router} />
		</React.StrictMode>
	);
}

main();
