import { Credentials, User, ProjectInfo, Project, ClimateRegion } from "./model";

export class Res<T> {

	private constructor(
		private readonly _val?: T,
		private readonly _err?: string
	) { }

	static ok<T>(val: T): Res<T> {
		return new Res(val, undefined);
	}

	static err<T>(err: string): Res<T> {
		return new Res(undefined as T, err)
	}

	get value(): T {
		if (typeof this._val === "undefined") {
			throw new Error("does not contain a value");
		}
		return this._val;
	}

	get isOk(): boolean {
		return typeof this.value !== "undefined";
	}

	get isErr(): boolean {
		return typeof this._err === "string";
	}

	get error(): string {
		if (!this._err) {
			throw new Error("does not contain an error");
		}
		return this._err;
	}

}

export async function postLogin(credentials: Credentials): Promise<Res<boolean>> {
	const r = await fetch("/api/users/login", {
		method: "POST",
		headers: {
			"Content-Type": "application/json",
		},
		body: JSON.stringify(credentials),
	});
	if (r.status === 200) {
		return Res.ok(true);
	}
	const msg = await r.text();
	return Res.err(`login failed: ${r.status} | ${msg}`);
}

export async function getCurrentUser(): Promise<Res<User>> {
	const r = await fetch("/api/users/current");
	if (r.status === 200) {
		const user = await r.json();
		return Res.ok(user);
	}
	const msg = await r.text();
	return Res.err(`failed to get current user: ${r.status} | ${msg}`);
}

export async function postLogout(): Promise<Res<boolean>> {
	const r = await fetch("/api/users/logout", {
		method: "POST",
	});
	if (r.status === 200) {
		return Res.ok(true);
	}
	const msg = await r.text();
	return Res.err(`failed to logout: ${r.status} | ${msg}`);
}

export async function getProjects(): Promise<Res<ProjectInfo[]>> {
	const r = await fetch("/api/projects");
	if (r.status === 200) {
		const projects = await r.json();
		return Res.ok(projects);
	}
	const msg = await r.text();
	return Res.err(`failed to get projects: ${r.status} | ${msg}`);
}

export async function createProject(
	name: string,
	description: string,
	file: File
): Promise<Res<ProjectInfo>> {

	const data = new FormData();
	data.append('name', name);
	data.append('description', description);
	data.append('file', file);

	const r = await fetch("/api/projects", {
		method: "POST",
		body: data,
	});

	if (r.status === 200) {
		const project = await r.json();
		return Res.ok(project);
	}
	const msg = await r.text();
	return Res.err(`failed to create project: ${r.status} | ${msg}`);
}

export async function getProject(id: number): Promise<Res<Project>> {
	const r = await fetch(`/api/projects/${id}`);
	if (r.status === 200) {
		const project = await r.json();
		return Res.ok(project);
	}
	if (r.status === 404) {
		return Res.err("project not found");
	}
	const msg = await r.text();
	return Res.err(`failed to get project: ${r.status} | ${msg}`);
}

export async function deleteProject(id: number): Promise<Res<boolean>> {
	const r = await fetch(`/api/projects/${id}`, {
		method: "DELETE",
	});
	if (r.status === 200) {
		return Res.ok(true);
	}
	const msg = await r.text();
	return Res.err(`failed to delete project: ${r.status} | ${msg}`);
}

export async function updateProject(project: Project): Promise<Res<ProjectInfo>> {
	const r = await fetch(`/api/projects/${project.id}`, {
		method: "POST",
		headers: {
			"Content-Type": "application/json",
		},
		body: JSON.stringify(project),
	});
	if (r.status === 200) {
		const info = await r.json();
		return Res.ok(info);
	}
	const msg = await r.text();
	return Res.err(`failed to update project: ${r.status} | ${msg}`);
}

export async function getClimateRegions(): Promise<Res<ClimateRegion[]>> {
	const r = await fetch("/api/climate-regions");
	if (r.status === 200) {
		const regions = await r.json();
		return Res.ok(regions);
	}
	const msg = await r.text();
	return Res.err(`failed to get climate regions: ${r.status} | ${msg}`);
}

