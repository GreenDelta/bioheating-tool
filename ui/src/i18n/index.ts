import i18n from "i18next";
import LanguageDetector from "i18next-browser-languagedetector";
import { initReactI18next } from "react-i18next";
import { en } from "./en";
import { de } from "./de";

export const LANGUAGES = ["en", "de"] as const;
export type Language = (typeof LANGUAGES)[number];

export function isLanguage(s: unknown): s is Language {
	return typeof s === "string" && (LANGUAGES as readonly string[]).includes(s);
}

// Language resolution: an explicit user setting (applied after login via
// `applyUserLanguage`) wins; otherwise the detector uses the last language
// stored in localStorage, then the browser language, then English.
i18n
	.use(LanguageDetector)
	.use(initReactI18next)
	.init({
		resources: {
			en: { translation: en },
			de: { translation: de },
		},
		supportedLngs: LANGUAGES,
		nonExplicitSupportedLngs: true, // de-DE, de-AT, ... -> de
		load: "languageOnly",
		fallbackLng: "en",
		interpolation: { escapeValue: false }, // React already escapes
		detection: {
			order: ["localStorage", "navigator"],
			lookupLocalStorage: "bioheating.language",
			caches: [],
		},
	});

const setHtmlLang = (lng: string) =>
	document.documentElement.setAttribute("lang", lng);
setHtmlLang(i18n.resolvedLanguage ?? "en");
i18n.on("languageChanged", setHtmlLang);

/// Returns the currently active UI language.
export function currentLanguage(): Language {
	const lng = i18n.resolvedLanguage;
	return isLanguage(lng) ? lng : "en";
}

/// Applies the language setting of a user. If the user has no explicit
/// setting, the browser language is detected again. The explicit setting is
/// also stored in localStorage so that the login page uses it next time.
export function applyUserLanguage(language?: string | null) {
	try {
		if (isLanguage(language)) {
			localStorage.setItem("bioheating.language", language);
		} else {
			localStorage.removeItem("bioheating.language");
		}
	} catch {
		// storage may be unavailable, e.g. in private mode
	}
	i18n.changeLanguage(isLanguage(language) ? language : undefined);
}

/// Translates an error code returned by the server (e.g. `projectNotFound`)
/// via the `errors.*` keys. Unknown codes (or plain messages from endpoints
/// that do not return codes yet) are returned unchanged.
export function errorText(code: string | null | undefined): string {
	if (!code) {
		return i18n.t("errors.unknown");
	}
	const key = `errors.${code}`;
	return i18n.exists(key) ? i18n.t(key as any) : code;
}

export default i18n;
