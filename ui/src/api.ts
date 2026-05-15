import {
	Credentials,
	User,
	UserData,
	ProjectInfo,
	Project,
	ClimateRegion,
	Fuel,
	TaskState,
	Solution
} from "./model";

interface ApiFetchOptions extends RequestInit {
	redirectOnUnauthorized?: boolean;
}

async function apiFetch(
	input: RequestInfo | URL,
	init?: ApiFetchOptions,
): Promise<Response> {
	const { redirectOnUnauthorized = true, ...requestInit } = init || {};
	const response = await fetch(input, requestInit);
	if (response.status === 401 && redirectOnUnauthorized) {
		window.location.assign("/ui/login");
		throw new Error("session expired");
	}
	return response;
}

export class Res<T> {
	private constructor(
		private readonly _val?: T,
		private readonly _err?: string,
	) { }

	static ok<T>(val: T): Res<T> {
		return new Res(val, undefined);
	}

	static err<T>(err: string): Res<T> {
		return new Res(undefined as T, err);
	}

	get value(): T {
		if (typeof this._val === "undefined") {
			throw new Error("does not contain a value");
		}
		return this._val;
	}

	get isOk(): boolean {
		return !this.isErr;
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

export async function postLogin(
	credentials: Credentials,
): Promise<Res<boolean>> {
	try {
		const r = await apiFetch("/api/users/login", {
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
	} catch (error) {
		return Res.err(`login failed: ${error}`);
	}
}

export async function getCurrentUser(): Promise<Res<User>> {
	try {
		const r = await apiFetch("/api/users/current", {
			redirectOnUnauthorized: false,
		});
		if (r.status === 200) {
			const user = await r.json();
			return Res.ok(user);
		}
		const msg = await r.text();
		return Res.err(`failed to get current user: ${r.status} | ${msg}`);
	} catch (error) {
		return Res.err(`failed to get current user: ${error}`);
	}
}

export async function postLogout(): Promise<Res<boolean>> {
	try {
		const r = await apiFetch("/api/users/logout", {
			method: "POST",
		});
		if (r.status === 200) {
			return Res.ok(true);
		}
		const msg = await r.text();
		return Res.err(`failed to logout: ${r.status} | ${msg}`);
	} catch (error) {
		return Res.err(`failed to logout: ${error}`);
	}
}

export async function getUser(id: number): Promise<Res<User>> {
	try {
		const r = await apiFetch(`/api/users/${id}`);
		if (r.status === 200) {
			const user = await r.json();
			return Res.ok(user);
		}
		const msg = await r.text();
		return Res.err(`failed to get user: ${r.status} | ${msg}`);
	} catch (error) {
		return Res.err(`failed to get user: ${error}`);
	}
}

export async function createUser(userData: UserData): Promise<Res<User>> {
	try {
		const r = await apiFetch("/api/users", {
			method: "POST",
			headers: {
				"Content-Type": "application/json",
			},
			body: JSON.stringify(userData),
		});
		if (r.status === 200) {
			const user = await r.json();
			return Res.ok(user);
		}
		const msg = await r.text();
		return Res.err(`failed to create user: ${r.status} | ${msg}`);
	} catch (error) {
		return Res.err(`failed to create user: ${error}`);
	}
}

export async function updateUser(
	id: number,
	userData: UserData,
): Promise<Res<User>> {
	try {
		const r = await apiFetch(`/api/users/${id}`, {
			method: "PUT",
			headers: {
				"Content-Type": "application/json",
			},
			body: JSON.stringify(userData),
		});
		if (r.status === 200) {
			const user = await r.json();
			return Res.ok(user);
		}
		const msg = await r.text();
		return Res.err(`failed to update user: ${r.status} | ${msg}`);
	} catch (error) {
		return Res.err(`failed to update user: ${error}`);
	}
}

export async function getUsers(): Promise<Res<User[]>> {
	try {
		const r = await apiFetch("/api/users");
		if (r.status === 200) {
			const users = await r.json();
			return Res.ok(users);
		}
		const msg = await r.text();
		return Res.err(`failed to get users: ${r.status} | ${msg}`);
	} catch (error) {
		return Res.err(`failed to get users: ${error}`);
	}
}

export async function deleteUser(id: number): Promise<Res<boolean>> {
	try {
		const r = await apiFetch(`/api/users/${id}`, {
			method: "DELETE",
		});
		if (r.status === 200) {
			return Res.ok(true);
		}
		const msg = await r.text();
		return Res.err(`failed to delete user: ${r.status} | ${msg}`);
	} catch (error) {
		return Res.err(`failed to delete user: ${error}`);
	}
}

export async function getProjects(): Promise<Res<ProjectInfo[]>> {
	try {
		const r = await apiFetch("/api/projects");
		if (r.status === 200) {
			const projects = await r.json();
			return Res.ok(projects);
		}
		const msg = await r.text();
		return Res.err(`failed to get projects: ${r.status} | ${msg}`);
	} catch (error) {
		return Res.err(`failed to get projects: ${error}`);
	}
}

interface NewProjectData {
	climateRegionId: number;
	name: string;
	description?: string;
	files: File[];
}

export async function createProject(
	d: NewProjectData,
): Promise<Res<TaskState>> {
	try {
		const data = new FormData();
		data.append("climateRegionId", d.climateRegionId.toString());
		data.append("name", d.name);
		for (const file of d.files) {
			data.append("file", file);
		}
		if (d.description) {
			data.append("description", d.description);
		}

		const r = await apiFetch("/api/projects", {
			method: "POST",
			body: data,
		});

		if (r.status === 200) {
			const state = await r.json();
			return Res.ok(state);
		}
		const msg = await r.text();
		return Res.err(`failed to create project: ${r.status} | ${msg}`);
	} catch (error) {
		return Res.err(`failed to create project: ${error}`);
	}
}

export async function getProject(id: number): Promise<Res<Project>> {
	try {
		const r = await apiFetch(`/api/projects/${id}`);
		if (r.status === 200) {
			const project = await r.json();
			return Res.ok(project);
		}
		if (r.status === 404) {
			return Res.err("project not found");
		}
		const msg = await r.text();
		return Res.err(`failed to get project: ${r.status} | ${msg}`);
	} catch (error) {
		return Res.err(`failed to get project: ${error}`);
	}
}

export async function deleteProject(id: number): Promise<Res<boolean>> {
	try {
		const r = await apiFetch(`/api/projects/${id}`, {
			method: "DELETE",
		});
		if (r.status === 200) {
			return Res.ok(true);
		}
		const msg = await r.text();
		return Res.err(`failed to delete project: ${r.status} | ${msg}`);
	} catch (error) {
		return Res.err(`failed to delete project: ${error}`);
	}
}

export async function updateProject(
	project: Project,
): Promise<Res<ProjectInfo>> {
	try {
		const r = await apiFetch(`/api/projects/${project.id}`, {
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
	} catch (error) {
		return Res.err(`failed to update project: ${error}`);
	}
}

export async function getClimateRegions(): Promise<Res<ClimateRegion[]>> {
	try {
		const r = await apiFetch("/api/climate-regions");
		if (r.status === 200) {
			const regions = await r.json();
			return Res.ok(regions);
		}
		const msg = await r.text();
		return Res.err(`failed to get climate regions: ${r.status} | ${msg}`);
	} catch (error) {
		return Res.err(`failed to get climate regions: ${error}`);
	}
}

export async function getFuels(): Promise<Res<Fuel[]>> {
	try {
		const r = await apiFetch("/api/fuels");
		if (r.status === 200) {
			const fuels = await r.json();
			return Res.ok(fuels);
		}
		const msg = await r.text();
		return Res.err(`failed to get fuels: ${r.status} | ${msg}`);
	} catch (error) {
		return Res.err(`failed to get fuels: ${error}`);
	}
}

export async function getSophenaPackage(
	solutionId: number,
): Promise<Res<boolean>> {
	try {
		const r = await apiFetch(`/api/solutions/${solutionId}/sophena-package`);
		if (r.status !== 200) {
			const msg = await r.text();
			return Res.err(
				`failed to download Sophena package: ${r.status} | ${msg}`,
			);
		}

		const blob = await r.blob();
		const url = window.URL.createObjectURL(blob);
		const a = document.createElement("a");
		a.href = url;
		a.download = fileNameOf(r);
		a.click();
		window.URL.revokeObjectURL(url);
		return Res.ok(true);
	} catch (error) {
		return Res.err(`failed to download Sophena package: ${error}`);
	}
}

export async function getSolutionXls(
	solutionId: number,
): Promise<Res<boolean>> {
	try {
		const r = await apiFetch(`/api/solutions/${solutionId}/xls`);
		if (r.status !== 200) {
			const msg = await r.text();
			return Res.err(
				`failed to download Excel file: ${r.status} | ${msg}`,
			);
		}

		const blob = await r.blob();
		const url = window.URL.createObjectURL(blob);
		const a = document.createElement("a");
		a.href = url;
		a.download = fileNameOf(r, `pipe-plan-${solutionId}.xlsx`);
		a.click();
		window.URL.revokeObjectURL(url);
		return Res.ok(true);
	} catch (error) {
		return Res.err(`failed to download Excel file: ${error}`);
	}
}

export async function exportProjectBuildingsXls(
	projectId: number,
	buildingIds: number[],
): Promise<Res<boolean>> {
	try {
		const r = await apiFetch(`/api/export/buildings-xls/${projectId}`, {
			method: "POST",
			headers: {
				"Content-Type": "application/json",
			},
			body: JSON.stringify(buildingIds),
		});
		if (r.status !== 200) {
			const msg = await r.text();
			return Res.err(
				`failed to export buildings: ${r.status} | ${msg}`,
			);
		}

		const blob = await r.blob();
		const url = window.URL.createObjectURL(blob);
		const a = document.createElement("a");
		a.href = url;
		a.download = fileNameOf(r, `project-${projectId}-buildings.xlsx`);
		a.click();
		window.URL.revokeObjectURL(url);
		return Res.ok(true);
	} catch (error) {
		return Res.err(`failed to export buildings: ${error}`);
	}
}

function fileNameOf(resp: Response, defaultName?: string): string {
	const header = resp.headers.get("content-disposition");
	if (header) {
		const match = header.match(/filename="(.+)"/);
		if (match) {
			return match[1];
		}
	}
	return defaultName || "project.sophena";
}

export async function getTaskState(id: string): Promise<Res<TaskState>> {
	try {
		const r = await apiFetch(`/api/tasks/${id}`);
		if (r.status === 200) {
			const state = await r.json();
			return Res.ok(state);
		}
		if (r.status === 404) {
			return Res.err("task not found");
		}
		const msg = await r.text();
		return Res.err(`failed to get task state: ${r.status} | ${msg}`);
	} catch (error) {
		return Res.err(`failed to get task state: ${error}`);
	}
}

export async function dropTaskResult(id: string): Promise<Res<boolean>> {
	try {
		const r = await apiFetch(`/api/tasks/${id}`, {
			method: "DELETE",
		});
		if (r.status === 200) {
			return Res.ok(true);
		}
		if (r.status === 404) {
			return Res.err("task not found");
		}
		const msg = await r.text();
		return Res.err(`failed to delete task: ${r.status} | ${msg}`);
	} catch (error) {
		return Res.err(`failed to delete task: ${error}`);
	}
}

export async function getSolution(id: number): Promise<Res<Solution>> {
	try {
		const r = await apiFetch(`/api/solutions/${id}`);
		if (r.status === 200) {
			const solution: Solution = await r.json();
			return Res.ok(solution);
		}
		if (r.status === 404) {
			return Res.err("solution not found");
		}
		const msg = await r.text();
		return Res.err(`failed to get solution: ${r.status} | ${msg}`);
	} catch (error) {
		return Res.err(`failed to get solution: ${error}`);
	}
}

export async function calculateSolution(projectId: number): Promise<Res<TaskState>> {
	try {
		const r = await apiFetch(`/api/solutions/project/${projectId}`, {
			method: "POST",
		});
		if (r.status === 200) {
			const task: TaskState = await r.json();
			return Res.ok(task);
		}
		const msg = await r.text();
		return Res.err(`failed to start calculation: ${r.status} | ${msg}`);
	} catch (error) {
		return Res.err(`failed to start calculation: ${error}`);
	}
}

export function getSolutionImageUrl(id: number): string {
	return `/api/solutions/${id}/image`;
}
