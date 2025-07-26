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
	};

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

	return (
		<div className="container-fluid">
			<div className="row">
				<div className="col-md-7 d-flex align-items-center">
					<div className="row justify-content-center w-100">
						<div className="col-md-8">
							<form>
								<ErrorRow err={error} />

								<div className="form-group row mt-3">
									<label className="col-sm-3 col-form-label col-form-label-lg">
										User
									</label>
									<div className="col-sm-9">
										<input
											type="text"
											className="form-control form-control-lg"
											required
											disabled={inProgress}
											value={userName}
											onChange={(e) => {
												setUserName(e.target.value);
												setError(null);
											}}
										/>
									</div>
								</div>

								<div className="form-group row mt-3">
									<label className="col-sm-3 col-form-label col-form-label-lg">
										Password
									</label>
									<div className="col-sm-9">
										<input
											type="password"
											className="form-control form-control-lg"
											required
											disabled={inProgress}
											value={password}
											onChange={(e) => {
												setPassword(e.target.value);
												setError(null);
											}}
										/>
									</div>
								</div>

								<div className="form-group row mt-3">
									<div className="col-sm-3" />
									<div className="col-sm-9">
										<button
											type="button"
											className="btn btn-lg btn-primary"
											onClick={() => onLogin()}
											disabled={inProgress}
										>
											Login
										</button>
									</div>
								</div>
							</form>
						</div>
					</div>
				</div>

				<div className="col-md-5">
					<img
						src="/img/home.png"
						alt="BIOHEATING"
						className="img-fluid rounded shadow mb-3"
					/>
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
