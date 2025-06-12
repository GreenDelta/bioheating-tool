import React, { useState } from "react";
import { useNavigate, useOutletContext } from "react-router-dom";
import * as api from "./api";
import { User } from "./model";

type UserContext = [User | null, (user: User) => void];

export const LoginPage = () => {

	const navigate = useNavigate();
	const [user, setUser] = useOutletContext<UserContext>();
	const [inProgress, setInProgress] = useState(false);
	const [userName, setUserName] = useState("");
	const [password, setPassword] = useState("");
	const [error, setError] = useState<string | null>(null);

	const onError = () => {
		setUserName("");
		setPassword("");
		setError("Wrong user name or password");
	}

	const onLogin = async () => {
		setInProgress(true);
		const res = await api.postLogin({ user: userName, password });

		setInProgress(false);
		if (res.isErr) {
			onError();
			return;
		}

		const u = await api.getCurrentUser();
		setInProgress(false);
		if (u.isErr) {
			onError();
			return;
		}

		setUser(u.value);
		navigate("/");
	};

	if (user) {
		navigate("/");
	}

	return <div className="row justify-content-center" style={{ paddingTop: "5%" }}>
		<div className="col-md-4">
			<form>
				<ErrorRow err={error} />

				<div className="mb-3">
					<label className="form-label">User</label>
					<input
						type="text"
						className="form-control"
						required
						disabled={inProgress}
						value={userName}
						onChange={e => {
							setUserName(e.target.value);
							setError(null);
						}}
					/>
				</div>

				<div className="mb-3">
					<label className="form-label">Password</label>
					<input
						type="password"
						className="form-control"
						required
						disabled={inProgress}
						value={password}
						onChange={e => {
							setPassword(e.target.value);
							setError(null);
						}}
					/>
				</div>

				<button
					type="button"
					className="btn btn-primary"
					onClick={() => onLogin()}
					disabled={inProgress}>
					Login
				</button>
			</form>
		</div>
	</div>;
}

const ErrorRow = ({ err }: { err: string | null }) => {
	if (!err) {
		return <></>;
	}
	return (
		<div className="alert alert-danger" role="alert">
			{err}
		</div>
	);
}
