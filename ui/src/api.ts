import { Credentials, User } from "./model";

export async function postLogin(credentials: Credentials): Promise<User> {
  const r = await fetch("/api/users/login", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(credentials),
  });
  if (r.status !== 200) {
    const message = await r.text();
    throw new Error(`failed to login: ${message}`);
  }
  return r.json();
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

