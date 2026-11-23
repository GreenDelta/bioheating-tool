/// The English translations. This is the reference language: all other
/// languages must provide the same keys (checked by the `Messages` type).
export const en = {
	common: {
		cancel: "Cancel",
		ok: "OK",
		update: "Update",
		home: "Home",
	},
	nav: {
		login: "Login",
		logout: "Logout",
		projects: "Projects",
		users: "Users",
		account: "Account",
	},
	login: {
		user: "User",
		password: "Password",
		submit: "Login",
		wrongCredentials: "Wrong user name or password",
	},
	settings: {
		title: "Settings",
		language: "Language",
		browserLanguage: "Browser language",
		languageHint:
			"The language of the user interface. By default, the language of the browser is used.",
		saveFailed: "Failed to save the language setting",
	},
	languages: {
		en: "English",
		de: "Deutsch",
	},
	// Error codes returned by the server; see `errorText` in `i18n/index.ts`.
	errors: {
		unknown: "An unknown error occurred",
	},
};

type Widen<T> = { [K in keyof T]: T[K] extends string ? string : Widen<T[K]> };

/// The structure of the messages that every language has to provide.
export type Messages = Widen<typeof en>;
