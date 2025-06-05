import { Credentials, User, Project, ProjectData } from "./model";

export class Res<T> {

	private constructor(
		private readonly _val?: T,
		private readonly _err?: string
	) {}

	static ok<T>(val: T): Res<T> {
		return new Res(val, undefined);
	}

	static err<T>(err: string): Res<T>  {
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

	get hasError(): boolean {
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

