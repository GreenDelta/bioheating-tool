import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import { en } from "./en";
import { de } from "./de";

export const LANGUAGES = ["en", "de"] as const;
export type Language = (typeof LANGUAGES)[number];

export function isLanguage(s: unknown): s is Language {
	return typeof s === "string" && (LANGUAGES as readonly string[]).includes(s);
}

const STORAGE_KEY = "bioheating.language";

/// The default language when the user has not selected one: German when it is
/// the language of the browser (`de`, `de-DE`, `de-AT`, ...), otherwise
/// English.
export function defaultLanguage(): Language {
	const lng = navigator.languages?.[0] ?? navigator.language ?? "";
	return lng.toLowerCase().split("-")[0] === "de" ? "de" : "en";
}

// The language that the user selected; it is stored in localStorage after
// login so that it is also used on the login page next time.
function storedLanguage(): Language | null {
	try {
		const lng = localStorage.getItem(STORAGE_KEY);
		return isLanguage(lng) ? lng : null;
	} catch {
		return null; // storage may be unavailable, e.g. in private mode
	}
}

i18n.use(initReactI18next).init({
	resources: {
		en: { translation: en },
		de: { translation: de },
	},
	lng: storedLanguage() ?? defaultLanguage(),
	supportedLngs: LANGUAGES,
	fallbackLng: "en",
	interpolation: { escapeValue: false }, // React already escapes
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

/// Applies the language setting of a user. If the user has not selected a
/// language yet, the default language is used, see `defaultLanguage`. The
/// selected language is also stored in localStorage so that the login page
/// uses it next time.
export function applyUserLanguage(language?: string | null) {
	try {
		if (isLanguage(language)) {
			localStorage.setItem(STORAGE_KEY, language);
		} else {
			localStorage.removeItem(STORAGE_KEY);
		}
	} catch {
		// storage may be unavailable, e.g. in private mode
	}
	i18n.changeLanguage(isLanguage(language) ? language : defaultLanguage());
}

/// Translates an error code returned by the server (e.g. `PROJECT_NOT_FOUND`,
/// see `ErrorCode.java`) via the `errors.*` keys. Unknown codes or plain
/// messages (e.g. network errors) are returned unchanged.
export function errorText(code: string | null | undefined): string {
	if (!code) {
		return i18n.t("errors.unknown");
	}
	const key = `errors.${code}`;
	return i18n.exists(key) ? i18n.t(key as any) : code;
}

/// Formats a number for the current UI language, e.g. `1,234.5` in English
/// and `1.234,5` in German.
export function formatNumber(
	value: number,
	options?: Intl.NumberFormatOptions,
): string {
	return value.toLocaleString(currentLanguage(), options);
}

/// Formats a date-time for the current UI language.
export function formatDateTime(value: string | number | Date): string {
	return new Date(value).toLocaleString(currentLanguage());
}

export default i18n;
