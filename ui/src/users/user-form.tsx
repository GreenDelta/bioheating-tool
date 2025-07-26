import React, { useState } from "react";
import { useNavigate, useOutletContext } from "react-router-dom";
import * as api from "../api";
import { User, UserData } from "../model";

interface FormData {
	name?: string;
	password?: string;
	confirmPassword?: string;
	fullName?: string;
	isAdmin: boolean;
	error?: string | null;
}

function useFormData() {
	const [data, setData] = useState<FormData>({
		name: "",
		password: "",
		confirmPassword: "",
		fullName: "",
		isAdmin: false,
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
	if (!data || !data.name || !data.password || !data.fullName) return false;
	const name = data.name.trim();
	const password = data.password.trim();
	const fullName = data.fullName.trim();
	const confirmPassword = data.confirmPassword?.trim() || "";

	return name.length >= 2 &&
		   password.length >= 3 &&
		   fullName.length > 0 &&
		   password === confirmPassword;
}

function getValidationError(data: FormData): string | null {
	if (!data.name?.trim() || data.name.trim().length < 2) {
		return "User name must be at least 2 characters long";
	}
	if (!data.password?.trim() || data.password.trim().length < 3) {
		return "Password must be at least 3 characters long";
	}
	if (!data.fullName?.trim()) {
		return "Full name is required";
	}
	if (data.password !== data.confirmPassword) {
		return "Passwords do not match";
	}
	return null;
}

export const UserForm = () => {
	const navigate = useNavigate();
	const [currentUser] = useOutletContext<[User]>();
	const { data, update } = useFormData();
	const [loading, setLoading] = useState(false);

	// Only admins can create users
	if (!currentUser.isAdmin) {
		return (
			<div className="alert alert-danger">
				<h4>Access Denied</h4>
				<p>Only administrators can create new users.</p>
			</div>
		);
	}

	const onOk = async () => {
		const validationError = getValidationError(data);
		if (validationError) {
			update({ error: validationError });
			return;
		}

		if (!isComplete(data)) {
			return;
		}

		setLoading(true);
		const userData: UserData = {
			name: data.name!.trim(),
			password: data.password!.trim(),
			fullName: data.fullName!.trim(),
			isAdmin: data.isAdmin,
		};

		const res = await api.createUser(userData);
		setLoading(false);

		if (res.isErr) {
			update({ error: res.error });
		} else {
			navigate("/ui/users");
		}
	};

	return (
		<div className="container-fluid">
			<div className="row">
				<div className="col-md-8">
					<h1>New User</h1>

					{data.error ? <ErrorRow err={data.error} /> : <></>}

					<div className="mb-3">
						<label className="form-label">Username *</label>
						<input
							type="text"
							className="form-control"
							value={data.name || ""}
							onChange={e => update({ name: e.target.value })}
							placeholder="Enter username (min. 2 characters)"
						/>
						<div className="form-text">
							This will be used for login. Must be at least 2 characters.
						</div>
					</div>

					<div className="mb-3">
						<label className="form-label">Full Name *</label>
						<input
							type="text"
							className="form-control"
							value={data.fullName || ""}
							onChange={e => update({ fullName: e.target.value })}
							placeholder="Enter full name"
						/>
					</div>

					<div className="mb-3">
						<label className="form-label">Password *</label>
						<input
							type="password"
							className="form-control"
							value={data.password || ""}
							onChange={e => update({ password: e.target.value })}
							placeholder="Enter password (min. 3 characters)"
						/>
						<div className="form-text">
							Must be at least 3 characters long.
						</div>
					</div>

					<div className="mb-3">
						<label className="form-label">Confirm Password *</label>
						<input
							type="password"
							className="form-control"
							value={data.confirmPassword || ""}
							onChange={e => update({ confirmPassword: e.target.value })}
							placeholder="Confirm password"
						/>
					</div>

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
							Administrators can manage users and have full access to the system.
						</div>
					</div>

					<div className="d-flex gap-2">
						<button
							className="btn btn-secondary"
							disabled={loading}
							onClick={() => navigate("/ui/users")}>
							Cancel
						</button>
						<button
							className="btn btn-primary"
							disabled={loading || !isComplete(data)}
							onClick={onOk}>
							{loading ? "Creating..." : "Create User"}
						</button>
					</div>
				</div>

				<div className="col-md-4">
					<div className="card">
						<div className="card-body">
							<h5 className="card-title">User Requirements</h5>
							<ul className="list-unstyled">
								<li className="mb-2">
									<span className={data.name && data.name.trim().length >= 2 ? "text-success" : "text-muted"}>
										✓ Username: min. 2 characters
									</span>
								</li>
								<li className="mb-2">
									<span className={data.fullName && data.fullName.trim().length > 0 ? "text-success" : "text-muted"}>
										✓ Full name: required
									</span>
								</li>
								<li className="mb-2">
									<span className={data.password && data.password.trim().length >= 3 ? "text-success" : "text-muted"}>
										✓ Password: min. 3 characters
									</span>
								</li>
								<li className="mb-2">
									<span className={data.password && data.confirmPassword && data.password === data.confirmPassword ? "text-success" : "text-muted"}>
										✓ Passwords match
									</span>
								</li>
							</ul>
						</div>
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
