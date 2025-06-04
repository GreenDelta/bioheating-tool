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
	const [error, setError] = useState<unknown>(null);

	const onLogin = async () => {
		setInProgress(true);
		const success = await api.postLogin({ user: userName, password });
		if (!success) {
			setInProgress(false);
			setError("Login failed");
			return;
		}
		const u = await api.getCurrentUser();
		setInProgress(false);
		if (!u) {
			setError("Login failed");
		} else {
			setUser(u);
			navigate("/");
		}
	};

	if (user) {
		navigate("/");
	}

	return <form style={{ paddingTop: "5%" }}>
		<div className="grid">
			<div />
			<label>
				User
				<input
					type="text"
					required
					disabled={inProgress}
					value={userName}
					onChange={e => setUserName(e.target.value)}></input>
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
					onChange={e => setPassword(e.target.value)}></input>
			</label>
			<div />
		</div>
		<div className="grid">
			<div />
			<button
				type="button" onClick={() => onLogin()}
				style={{ marginTop: 10, justifySelf: "start", width: "33%" }}>
				Login
			</button>
			<div />
		</div>
	</form>;
}
