import {
	Credentials,
	User,
	UserData,
	ProjectInfo,
	Project,
	ClimateRegion,
	Fuel,
	TaskState,
	Solution,
} from "./model";

function jsonRequest(method: string, body: unknown): RequestInit {
	const headers = new Headers();
	headers.set("Content-Type", "application/json");
	return {
		method,
		headers,
		body: JSON.stringify(body),
	};
}

async function request<T>(
	input: RequestInfo | URL,
	decode: (response: Response) => Promise<T>,
	init?: RequestInit,
): Promise<Res<T>> {
	try {
		const response = await fetch(input, init);
		if (response.status === 401) {
			window.location.assign("/ui/login");
			throw new Error("session expired");
		}
		if (response.status === 200) {
			return Res.ok(await decode(response));
		}
		const err = await response.text();
		return Res.err(err || `HTTP ${response.status}`);
	} catch (error) {
		const err = error instanceof Error ? error.message : String(error);
		return Res.err(err);
	}
}

function getJson<T>(
	input: RequestInfo | URL,
	init?: RequestInit,
): Promise<Res<T>> {
	return request(input, response => response.json() as Promise<T>, init);
}

function sendRequest(
	input: RequestInfo | URL,
	init?: RequestInit,
): Promise<Res<boolean>> {
	return request(input, async () => true, init);
}

export class Res<T> {
	private constructor(
		private readonly _val?: T,
		private readonly _err?: string,
	) {}

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
	return sendRequest("/api/users/login", jsonRequest("POST", credentials));
}

export async function getCurrentUser(): Promise<Res<User>> {
	try {
		const response = await fetch("/api/users/current");
		if (response.status === 200) {
			return Res.ok(await response.json());
		}
		const err = await response.text();
		return Res.err(err || `HTTP ${response.status}`);
	} catch (error) {
		const err = error instanceof Error ? error.message : String(error);
		return Res.err(err);
	}
}

export async function postLogout(): Promise<Res<boolean>> {
	return sendRequest("/api/users/logout", {
		method: "POST",
	});
}

export async function getUser(id: number): Promise<Res<User>> {
	return getJson<User>(`/api/users/${id}`);
}

export async function createUser(userData: UserData): Promise<Res<User>> {
	return getJson<User>("/api/users", jsonRequest("POST", userData));
}

export async function updateUser(
	id: number,
	userData: UserData,
): Promise<Res<User>> {
	return getJson<User>(`/api/users/${id}`, jsonRequest("PUT", userData));
}

export async function getUsers(): Promise<Res<User[]>> {
	return getJson<User[]>("/api/users");
}

export async function deleteUser(id: number): Promise<Res<boolean>> {
	return sendRequest(`/api/users/${id}`, {
		method: "DELETE",
	});
}

export async function getProjects(): Promise<Res<ProjectInfo[]>> {
	return getJson<ProjectInfo[]>("/api/projects");
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
	const data = new FormData();
	data.append("climateRegionId", d.climateRegionId.toString());
	data.append("name", d.name);
	for (const file of d.files) {
		data.append("file", file);
	}
	if (d.description) {
		data.append("description", d.description);
	}

	return getJson<TaskState>("/api/projects", {
		method: "POST",
		body: data,
	});
}

export async function getProject(id: number): Promise<Res<Project>> {
	return getJson<Project>(`/api/projects/${id}`);
}

export async function deleteProject(id: number): Promise<Res<boolean>> {
	return sendRequest(`/api/projects/${id}`, {
		method: "DELETE",
	});
}

export async function updateProject(
	project: Project,
): Promise<Res<ProjectInfo>> {
	return getJson<ProjectInfo>(
		`/api/projects/${project.id}`,
		jsonRequest("POST", project),
	);
}

export async function getClimateRegions(): Promise<Res<ClimateRegion[]>> {
	return getJson<ClimateRegion[]>("/api/climate-regions");
}

export async function getFuels(): Promise<Res<Fuel[]>> {
	return getJson<Fuel[]>("/api/fuels");
}

async function download(
	input: RequestInfo | URL,
	defaultName: string,
	init?: RequestInit,
): Promise<Res<boolean>> {
	return request(
		input,
		async response => {
			const blob = await response.blob();
			const url = window.URL.createObjectURL(blob);
			const a = document.createElement("a");
			a.href = url;
			a.download = fileNameOf(response, defaultName);
			a.click();
			window.URL.revokeObjectURL(url);
			return true;
		},
		init,
	);
}

export async function getSophenaPackage(
	solutionId: number,
): Promise<Res<boolean>> {
	return download(
		`/api/solutions/${solutionId}/sophena-package`,
		"project.sophena",
	);
}

export async function getSolutionXls(
	solutionId: number,
): Promise<Res<boolean>> {
	return download(
		`/api/solutions/${solutionId}/xls`,
		`pipe-plan-${solutionId}.xlsx`,
	);
}

export async function exportProjectBuildingsXls(
	projectId: number,
	buildingIds: number[],
): Promise<Res<boolean>> {
	return download(
		`/api/export/buildings-xls/${projectId}`,
		`project-${projectId}-buildings.xlsx`,
		jsonRequest("POST", buildingIds),
	);
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
	return getJson<TaskState>(`/api/tasks/${id}`);
}

export async function dropTaskResult(id: string): Promise<Res<boolean>> {
	return sendRequest(`/api/tasks/${id}`, {
		method: "DELETE",
	});
}

export async function getSolution(id: number): Promise<Res<Solution>> {
	return getJson<Solution>(`/api/solutions/${id}`);
}

export async function calculateSolution(
	projectId: number,
): Promise<Res<TaskState>> {
	return getJson<TaskState>(`/api/solutions/project/${projectId}`, {
		method: "POST",
	});
}

export function getSolutionImageUrl(id: number): string {
	return `/api/solutions/${id}/image`;
}
