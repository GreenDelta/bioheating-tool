import {
	Credentials,
	User,
	UserData,
	ProjectInfo,
	Project,
	ClimateRegion,
	Fuel,
} from "./model";

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

export async function postLogin(
	credentials: Credentials,
): Promise<Res<boolean>> {
	try {
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
	} catch (error) {
		return Res.err(`login failed: ${error}`);
	}
}

export async function getCurrentUser(): Promise<Res<User>> {
	try {
		const r = await fetch("/api/users/current");
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
		const r = await fetch("/api/users/logout", {
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
		const r = await fetch(`/api/users/${id}`);
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
		const r = await fetch("/api/users", {
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
		const r = await fetch(`/api/users/${id}`, {
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
		const r = await fetch("/api/users");
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

export async function getProjects(): Promise<Res<ProjectInfo[]>> {
	try {
		const r = await fetch("/api/projects");
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
	fuelId: number;
	name: string;
	description?: string;
	file: File;
}

export async function createProject(
	d: NewProjectData,
): Promise<Res<ProjectInfo>> {
	try {
		const data = new FormData();
		data.append("climateRegionId", d.climateRegionId.toString());
		data.append("fuelId", d.fuelId.toString());
		data.append("name", d.name);
		data.append("file", d.file);
		if (d.description) {
			data.append("description", d.description);
		}

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
	} catch (error) {
		return Res.err(`failed to create project: ${error}`);
	}
}

export async function getProject(id: number): Promise<Res<Project>> {
	try {
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
	} catch (error) {
		return Res.err(`failed to get project: ${error}`);
	}
}

export async function deleteProject(id: number): Promise<Res<boolean>> {
	try {
		const r = await fetch(`/api/projects/${id}`, {
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
	} catch (error) {
		return Res.err(`failed to update project: ${error}`);
	}
}

export async function getClimateRegions(): Promise<Res<ClimateRegion[]>> {
	try {
		const r = await fetch("/api/climate-regions");
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
		const r = await fetch("/api/fuels");
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
	projectId: number,
): Promise<Res<boolean>> {
	try {
		const r = await fetch(`/api/projects/${projectId}/sophena-package`);
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

function fileNameOf(resp: Response): string {
	const header = resp.headers.get("content-disposition");
	if (header) {
		const match = header.match(/filename="(.+)"/);
		if (match) {
			return match[1];
		}
	}
	return "project.sophena";
}
