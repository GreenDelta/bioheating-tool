import type { Messages } from "./en";

export const de: Messages = {
	common: {
		cancel: "Abbrechen",
		ok: "OK",
		update: "Aktualisieren",
		home: "Start",
	},
	nav: {
		login: "Anmelden",
		logout: "Abmelden",
		projects: "Projekte",
		users: "Benutzer",
		account: "Konto",
	},
	login: {
		user: "Benutzer",
		password: "Passwort",
		submit: "Anmelden",
		wrongCredentials: "Falscher Benutzername oder falsches Passwort",
	},
	settings: {
		title: "Einstellungen",
		language: "Sprache",
		browserLanguage: "Browsersprache",
		languageHint:
			"Die Sprache der Benutzeroberfläche. Standardmäßig wird die Sprache des Browsers verwendet.",
		saveFailed: "Die Spracheinstellung konnte nicht gespeichert werden",
	},
	languages: {
		en: "English",
		de: "Deutsch",
	},
	errors: {
		unknown: "Ein unbekannter Fehler ist aufgetreten",
	},
};
