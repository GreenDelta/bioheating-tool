import React, { useState } from "react";
import { useNavigate, useOutletContext, useLoaderData } from "react-router-dom";
import { useTranslation } from "react-i18next";
import type { ParseKeys } from "i18next";
import * as api from "../api";
import { errorText } from "../i18n";
import { User, UserData } from "../model";
import { FluidBreadcrumbRow } from "../components/navi";

interface FormData {
	name?: string;
	password?: string;
	confirmedPassword?: string;
	fullName?: string;
	isAdmin: boolean;
	error?: string | null;
}

function useFormData(initialUser?: User) {
	const [data, setData] = useState<FormData>({
		name: initialUser?.name || "",
		password: "",
		confirmedPassword: "",
		fullName: initialUser?.fullName || "",
		isAdmin: initialUser?.isAdmin || false,
	});

	const update = (diff: Partial<FormData>) => {
		if (diff.error) {
			setData(prev => ({ ...prev, error: diff.error }));
		} else {
			setData(prev => ({ ...prev, ...diff, error: null }));
		}
	};

	return { data, update };
}

function isComplete(data: FormData): boolean {
	if (!data || !data.name || !data.password || !data.fullName) {
		return false;
	}
	const name = data.name.trim();
	const password = data.password.trim();
	const fullName = data.fullName.trim();
	const confirmedPassword = data.confirmedPassword?.trim() || "";

	return (
		name.length >= 2 &&
		password.length >= 3 &&
		fullName.length > 0 &&
		password === confirmedPassword
	);
}

/// Returns the translation key of the validation error, if any.
function validate(data: FormData): ParseKeys | null {
	if (!data.name?.trim() || data.name.trim().length < 2) {
		return "userForm.nameTooShort";
	}
	if (!data.password?.trim() || data.password.trim().length < 3) {
		return "userForm.passwordTooShort";
	}
	if (!data.fullName?.trim()) {
		return "userForm.fullNameRequired";
	}
	if (data.password !== data.confirmedPassword) {
		return "userForm.passwordsMismatch";
	}
	return null;
}

export const UserForm = () => {
	const { t } = useTranslation();
	const navigate = useNavigate();
	const [currentUser] = useOutletContext<[User]>();
	const existingUser = useLoaderData() as User | undefined;
	const isEdit = !!existingUser;
	const isSelfEdit = !!existingUser && existingUser.id === currentUser.id;
	const isRestrictedSelfEdit = isSelfEdit && !currentUser.isAdmin;
	const { data, update } = useFormData(existingUser);
	const [loading, setLoading] = useState(false);

	if (!currentUser.isAdmin && !isRestrictedSelfEdit) {
		return (
			<div className="alert alert-danger">
				<h4>{t("userForm.accessDenied")}</h4>
				<p>
					{isEdit ? t("userForm.onlyAdminsEdit") : t("userForm.onlyAdminsCreate")}
				</p>
			</div>
		);
	}

	const onOk = async () => {
		const err = validate(data);
		if (err) {
			update({ error: t(err) });
			return;
		}

		setLoading(true);
		const userData: UserData = {
			name: data.name!.trim(),
			password: data.password!.trim(),
			fullName: data.fullName!.trim(),
			isAdmin: isRestrictedSelfEdit
				? existingUser.isAdmin
				: data.isAdmin,
		};

		const res = isEdit
			? await api.updateUser(existingUser.id, userData)
			: await api.createUser(userData);
		setLoading(false);

		if (res.isErr) {
			update({ error: errorText(res.error) });
		} else {
			navigate(isRestrictedSelfEdit ? "/" : "/ui/users");
		}
	};

	return (
		<div className="container-fluid">
			<FluidBreadcrumbRow
				active={
					isRestrictedSelfEdit
						? t("nav.account")
						: isEdit
							? t("userForm.edit")
							: t("common.new")
				}
				path={isRestrictedSelfEdit
					? [["/", t("common.home")]]
					: [
						["/", t("common.home")],
						["/ui/users", t("users.title")],
					]}
			/>
			<div className="row justify-content-center">
				<div className="col-12 col-md-8 col-lg-6">

					{data.error ? <ErrorRow err={data.error} /> : <></>}

					<div className="mb-3">
						<label className="form-label">{t("userForm.userName")}</label>
						<input
							type="text"
							className="form-control"
							value={data.name || ""}
							onChange={e => update({ name: e.target.value })}
						/>
						<div className="form-text">{t("userForm.userNameHint")}</div>
					</div>

					<div className="mb-3">
						<label className="form-label">{t("userForm.fullName")}</label>
						<input
							type="text"
							className="form-control"
							value={data.fullName || ""}
							onChange={e => update({ fullName: e.target.value })}
						/>
					</div>

					<div className="mb-3">
						<label className="form-label">{t("userForm.password")}</label>
						<input
							type="password"
							className="form-control"
							value={data.password || ""}
							onChange={e => update({ password: e.target.value })}
						/>
					</div>

					<div className="mb-3">
						<label className="form-label">{t("userForm.confirmPassword")}</label>
						<input
							type="password"
							className="form-control"
							value={data.confirmedPassword || ""}
							onChange={e => update({ confirmedPassword: e.target.value })}
						/>
					</div>

					{!isRestrictedSelfEdit && (
						<div className="mb-3">
							<div className="form-check">
								<input
									className="form-check-input"
									type="checkbox"
									checked={data.isAdmin}
									onChange={e => update({ isAdmin: e.target.checked })}
									id="adminCheck"
								/>
								<label className="form-check-label" htmlFor="adminCheck">
									{t("userForm.administrator")}
								</label>
							</div>
							<div className="form-text">{t("userForm.administratorHint")}</div>
						</div>
					)}

					<div className="d-flex gap-2 justify-content-end">
						<button
							className="btn btn-outline-secondary"
							disabled={loading}
							onClick={() => navigate(isRestrictedSelfEdit ? "/ui/projects" : "/ui/users")}
							style={{ width: 120 }}>
							{t("common.cancel")}
						</button>
						<button
							className="btn btn-outline-primary"
							disabled={loading || !isComplete(data)}
							onClick={onOk}
							style={{ width: 120 }}>
							{isEdit ? t("common.update") : t("common.ok")}
						</button>
					</div>
				</div>
			</div>
		</div>
	);
};

const ErrorRow = ({ err }: { err: string | null }) => {
	if (!err) {
		return <></>;
	}
	return (
		<div className="alert alert-danger" role="alert">
			{err}
		</div>
	);
};
