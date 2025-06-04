import { Credentials, User, Project, ProjectData } from "./model";

export async function postLogin(credentials: Credentials): Promise<boolean> {
  const r = await fetch("/api/users/login", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(credentials),
  });
	return r.status === 200;
}

export async function getCurrentUser(): Promise<User | null> {
	const r = await fetch("/api/users/current");
	return r.status === 200 ? r.json() : null;
}

export async function postLogout(): Promise<void> {
	const r = await fetch("/api/users/logout", {
		method: "POST",
	});
	if (r.status !== 200) {
		const message = await r.text();
		throw new Error(`failed to logout: ${message}`);
	}
}

export async function getProjects(): Promise<Project[]> {
	const r = await fetch("/api/projects");
	if (r.status !== 200) {
		throw new Error(`failed to fetch projects: ${r.statusText}`);
	}
	return r.json();
}

export async function createProject(data: ProjectData): Promise<Project> {
	const r = await fetch("/api/projects", {
		method: "POST",
		headers: {
			"Content-Type": "application/json",
		},
		body: JSON.stringify(data),
	});
	if (r.status !== 200) {
		const message = await r.text();
		throw new Error(`failed to create project: ${message}`);
	}
	return r.json();
}

export async function getProject(id: number): Promise<Project | null> {
	const r = await fetch(`/api/projects/${id}`);
	return r.status === 200 ? r.json() : null;
}

export async function deleteProject(id: number): Promise<void> {
	const r = await fetch(`/api/projects/${id}`, {
		method: "DELETE",
	});
	if (r.status !== 200) {
		const message = await r.text();
		throw new Error(`failed to delete project: ${message}`);
	}
}

