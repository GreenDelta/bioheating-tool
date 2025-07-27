import React, { useState } from "react";
import { useNavigate, useOutletContext } from "react-router-dom";
import * as api from "./api";
import { User } from "./model";

type UserContext = [User | null, (user: User) => void];

interface LoginContext {
	user: User | null;
	isProgressing: boolean;

	userName: string;
	setUserName: (name: string) => void;

	password: string;
	setPassword: (pw: string) => void;

	error: string | null;
	forward: () => void;
	onLogin: () => void;
	onKeyDown: (e: React.KeyboardEvent) => void;
}


function useLoginContext(): LoginContext {
	const navigate = useNavigate();
	const [user, setUser] = useOutletContext<UserContext>();
	const [isProgressing, setProgressing] = useState(false);
	const [userName, _setUserName] = useState("");
	const [password, _setPassword] = useState("");
	const [error, _setError] = useState<string | null>(null);

	const setError = () => {
		_setUserName("");
		_setPassword("");
		_setError("Wrong user name or password");
	};

	const setUserName = (name: string) => {
		_setError(null);
		_setUserName(name);
	}

	const setPassword = (pw: string) => {
		_setError(null);
		_setPassword(pw);
	}

	const onLogin = async () => {
		setProgressing(true);
		const res = await api.postLogin({ user: userName, password });

		if (res.isErr) {
			setProgressing(false);
			setError();
			return;
		}

		const u = await api.getCurrentUser();
		setProgressing(false);
		if (u.isErr) {
			setError();
		} else {
			setUser(u.value);
			navigate("/");
		}
	};

	const onKeyDown = (e: React.KeyboardEvent) => {
		if (e.key === "Enter" && !isProgressing && userName && password) {
			onLogin();
		}
	};

	return {
		user,
		isProgressing,
		userName,
		setUserName,
		password,
		setPassword,
		error,
		forward: () => navigate("/"),
		onLogin,
		onKeyDown
	}
}


export const LoginPage = () => {
	const ctx = useLoginContext();
	if (ctx.user) {
		ctx.forward();
	}

	return (
		<div className="container-fluid">
			<div className="row">
				<div className="col-md-7 d-flex align-items-center">
					<div className="row justify-content-center w-100">
						<div className="col-md-8">
							<form>
								<ErrorRow ctx={ctx} />
								<UserRow ctx={ctx} />
								<PasswordRow ctx={ctx} />
								<LoginButton ctx={ctx} />
							</form>
						</div>
					</div>
				</div>
				<div className="col-md-5">
					<img
						src="/img/home.png"
						alt="BIOHEATING"
						className="img-fluid rounded shadow mb-3" />
				</div>
			</div>
		</div>
	);
};


type Props = { ctx: LoginContext };


const ErrorRow = ({ ctx }: Props) => {
	if (!ctx.error) {
		return null;
	}
	return (
		<div className="alert alert-danger" role="alert">
			{ctx.error}
		</div>
	);
};


const UserRow = ({ ctx }: Props) => {
	return (
		<div className="form-group row mt-3">
			<label className="col-sm-3 col-form-label col-form-label-lg">
				User
			</label>
			<div className="col-sm-9">
				<input
					type="text"
					className="form-control form-control-lg"
					required
					disabled={ctx.isProgressing}
					value={ctx.userName}
					onChange={e => ctx.setUserName(e.target.value)}
					onKeyDown={ctx.onKeyDown}
				/>
			</div>
		</div>
	);
};


const PasswordRow = ({ ctx }: Props) => {
	return (
		<div className="form-group row mt-3">
			<label className="col-sm-3 col-form-label col-form-label-lg">
				Password
			</label>
			<div className="col-sm-9">
				<input
					type="password"
					className="form-control form-control-lg"
					required
					disabled={ctx.isProgressing}
					value={ctx.password}
					onChange={e => ctx.setPassword(e.target.value)}
					onKeyDown={ctx.onKeyDown}
				/>
			</div>
		</div>
	);
};


const LoginButton = ({ ctx }: Props) => {
	return (
		<div className="form-group row mt-3">
			<div className="col-sm-3" />
			<div className="col-sm-9">
				<button
					type="button"
					className="btn btn-lg btn-outline-primary"
					onClick={() => ctx.onLogin()}
					disabled={ctx.isProgressing}>
					Login
				</button>
			</div>
		</div>
	);
};
