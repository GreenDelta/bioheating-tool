import React, { useState } from "react";
import { useNavigate, useOutletContext, useLoaderData } from "react-router-dom";
import * as api from "../api";
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

function validate(data: FormData): string | null {
	if (!data.name?.trim() || data.name.trim().length < 2) {
		return "User name is too short";
	}
	if (!data.password?.trim() || data.password.trim().length < 3) {
		return "Password is too short";
	}
	if (!data.fullName?.trim()) {
		return "Full name is required";
	}
	if (data.password !== data.confirmedPassword) {
		return "Passwords do not match";
	}
	return null;
}

export const UserForm = () => {
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
				<h4>Access Denied</h4>
				<p>Only administrators can {isEdit ? 'edit' : 'create'} users.</p>
			</div>
		);
	}

	const onOk = async () => {
		const err = validate(data);
		if (err) {
			update({ error: err });
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
			update({ error: res.error });
		} else {
			navigate(isRestrictedSelfEdit ? "/" : "/ui/users");
		}
	};

	return (
		<div className="container-fluid">
			<FluidBreadcrumbRow
				active={isRestrictedSelfEdit ? "Account" : isEdit ? "Edit" : "New"}
				path={isRestrictedSelfEdit
					? [
						["/", "Home"],
						["/ui/account", "Account"],
					]
					: [
						["/", "Home"],
						["/ui/users", "Users"],
					]}
			/>
			<div className="row justify-content-center">
				<div className="col-12 col-md-8 col-lg-6">

					{data.error ? <ErrorRow err={data.error} /> : <></>}

					<div className="mb-3">
						<label className="form-label">User name</label>
						<input
							type="text"
							className="form-control"
							value={data.name || ""}
							onChange={e => update({ name: e.target.value })}
						/>
						<div className="form-text">This will be used for the login</div>
					</div>

					<div className="mb-3">
						<label className="form-label">Full name</label>
						<input
							type="text"
							className="form-control"
							value={data.fullName || ""}
							onChange={e => update({ fullName: e.target.value })}
						/>
					</div>

					<div className="mb-3">
						<label className="form-label">Password</label>
						<input
							type="password"
							className="form-control"
							value={data.password || ""}
							onChange={e => update({ password: e.target.value })}
						/>
					</div>

					<div className="mb-3">
						<label className="form-label">Confirm password</label>
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
									Administrator
								</label>
							</div>
							<div className="form-text">
								Administrators can manage users and have full access to the
								system.
							</div>
						</div>
					)}

					<div className="d-flex gap-2 justify-content-end">
						<button
							className="btn btn-outline-secondary"
							disabled={loading}
							onClick={() => navigate(isRestrictedSelfEdit ? "/ui/projects" : "/ui/users")}
							style={{ width: 120 }}>
							Cancel
						</button>
						<button
							className="btn btn-outline-primary"
							disabled={loading || !isComplete(data)}
							onClick={onOk}
							style={{ width: 120 }}>
							{isEdit ? "Update" : "OK"}
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
