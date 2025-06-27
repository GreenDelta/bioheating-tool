export enum Inclusion {
	OPTIONAL = "OPTIONAL",
	REQUIRED = "REQUIRED",
	EXCLUDED = "EXCLUDED"
}

export function inclusionFromString(value: string): Inclusion {
	switch (value?.toUpperCase()) {
		case "REQUIRED":
			return Inclusion.REQUIRED;
		case "EXCLUDED":
			return Inclusion.EXCLUDED;
		case "OPTIONAL":
		default:
			return Inclusion.OPTIONAL;
	}
}

export function inclusionToString(inclusion: Inclusion): string {
	return inclusion.toString();
}
