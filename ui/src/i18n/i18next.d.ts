import "i18next";
import type { en } from "./en";

// Makes translation keys type-safe: `t("nav.typo")` is a compile error.
declare module "i18next" {
	interface CustomTypeOptions {
		defaultNS: "translation";
		resources: { translation: typeof en };
	}
}
