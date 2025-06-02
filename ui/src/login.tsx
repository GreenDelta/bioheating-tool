import React, { useState } from "react";
import { useNavigate, useOutletContext } from "react-router-dom";
import * as api from "./api";
import { User } from "./model";

type UserContext =[User | null, (user: User) => void];

export const LoginPage = () => {

  const navigate = useNavigate();
  const [user, setUser] = useOutletContext<UserContext>();
  const [inProgress, setInProgress] = useState(false);
  const [userName, setUserName] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<unknown>(null);

  const onLogin = async () => {
    setInProgress(true);
    try {
      const u = await api.postLogin({ user: userName, password });
      setUser(u);
      setInProgress(false);
    } catch (err: unknown) {
      console.log(err);
      setError(err);
      setInProgress(false);
    }
  };

  if (user) {
    navigate("/");
  }

  return <>
    <article>
      <form>
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
          <button type="button" onClick={() => onLogin()}>Login</button>
          <div />
        </div>
      </form>
    </article>
  </>;
}
