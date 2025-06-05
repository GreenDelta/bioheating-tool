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

	return <form style={{ paddingTop: "5%" }}>

		<ErrorRow err={error} />

		<div className="grid">
			<div />
			<label>
				User
				<input
					type="text"
					required
					disabled={inProgress}
					value={userName}
					onChange={e => {
						setUserName(e.target.value);
						setError(null);
					}}></input>
			</label>
			<div />
		</div>

		<div className="grid">
			<div />
			<label>
				Password
				<input
					type="password"
					required
					disabled={inProgress}
					value={password}
					onChange={e => {
						setPassword(e.target.value);
						setError(null);
					}}></input>
			</label>
			<div />
		</div>

		<div className="grid">
			<div />
			<button
				type="button" onClick={() => onLogin()}
				style={{ marginTop: 10, justifySelf: "start", width: "33%" }}
				disabled={inProgress}>
				Login
			</button>
			<div />
		</div>

	</form>;
}

const ErrorRow = ({ err }: { err: string | null }) => {
	if (!err) {
		return <></>;
	}
	return (
		<div>
			<p style={{ textAlign: "center", color: "var(--pico-del-color)" }}>{err}</p>
		</div>
	);
}
