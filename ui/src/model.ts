export interface User {
	name: string;
	fullName: string;
	isAdmin: boolean;
}

export interface Credentials {
	user: string;
	password: string;
}

export interface Project {
	id: number;
	name: string;
	description: string;
}

export interface ProjectData {
	name: string;
	description: string;
}
