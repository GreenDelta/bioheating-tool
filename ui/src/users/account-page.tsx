import React, { useState } from "react";
import { useOutletContext } from "react-router-dom";
import { useTranslation } from "react-i18next";
import * as api from "../api";
import { User } from "../model";
import { LANGUAGES, isLanguage } from "../i18n";
import { UserForm } from "./user-form";

/// The account page of the current user: the user data form and the
/// personal settings.
export const AccountPage = () => {
	return (
		<>
			<UserForm />
			<div className="row justify-content-center mt-4">
				<div className="col-12 col-md-8 col-lg-6">
					<LanguageSettings />
				</div>
			</div>
		</>
	);
};

const LanguageSettings = () => {
	const { t } = useTranslation();
	const [user, setUser] = useOutletContext<[User, (u: User) => void]>();
	const [saving, setSaving] = useState(false);
	const [error, setError] = useState<string | null>(null);
	const value = isLanguage(user.language) ? user.language : "";

	const onChange = async (lang: string) => {
		setSaving(true);
		setError(null);
		const res = await api.setLanguage(lang || null);
		setSaving(false);
		if (res.isErr) {
			setError(t("settings.saveFailed"));
		} else {
			// updates the user in the root component which then applies the
			// language; see `applyUserLanguage`
			setUser(res.value);
		}
	};

	return (
		<>
			<h5 className="border-bottom pb-2">{t("settings.title")}</h5>
			{error && (
				<div className="alert alert-danger" role="alert">
					{error}
				</div>
			)}
			<div className="mb-3">
				<label className="form-label" htmlFor="languageSelect">
					{t("settings.language")}
				</label>
				<select
					id="languageSelect"
					className="form-select"
					value={value}
					disabled={saving}
					onChange={e => onChange(e.target.value)}>
					<option value="">{t("settings.browserLanguage")}</option>
					{LANGUAGES.map(lang => (
						<option key={lang} value={lang}>
							{t(`languages.${lang}`)}
						</option>
					))}
				</select>
				<div className="form-text">{t("settings.languageHint")}</div>
			</div>
		</>
	);
};
