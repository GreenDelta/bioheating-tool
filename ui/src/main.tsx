import React, { useState, useEffect } from 'react';
import { createRoot } from 'react-dom/client';
import { useNavigate, Outlet, Link, RouterProvider, createBrowserRouter, useParams } from 'react-router-dom';
import { User } from './model';
import * as api from './api';
import { LoginPage } from './login';
import { ProjectList, ProjectForm, ProjectDetail } from './projects';

const Root = () => {
  const navigate = useNavigate();
  const [user, setUser] = React.useState<User | null>(null);
  useEffect(() => {
    api.getCurrentUser().then(setUser);
  }, [])

  const onLogout = () => {
    api.postLogout().then(() => setUser(null));
    navigate("/");
  };
  return <>
    <MainMenu user={user} onLogout={onLogout} />
    <Outlet context={[user, setUser]} />
  </>;
}

const MainMenu = (props: { user: User | null, onLogout: () => void }) => {

  if (!props.user) {
    return (
      <nav style={{ marginBottom: 30 }}>
        <ul>
          <li><Link to="/">BIOHEATING</Link></li>
        </ul>
        <ul>
          <li><Link to="/ui/login">Login</Link></li>
        </ul>
      </nav>
    );
  }

  return (
    <nav style={{ marginBottom: 30 }}>
      <ul>
        <li><Link to="/">BIOHEATING</Link></li>
        <li></li>
        <li><Link to="/ui/projects">projects</Link></li>
        <li><Link to="/ui/users">users</Link></li>
      </ul>
      <ul>
        <li>
          <a onClick={props.onLogout}>Logout</a>
        </li>
      </ul>
    </nav>
  );
};

const ProjectDetailWrapper = () => {
  const { id } = useParams();
  return <ProjectDetail projectId={parseInt(id || '0', 10)} />;
};

function main() {
  const router = createBrowserRouter([
    {
      path: "/",
      element: <Root />,
      children: [
        {
          path: "/ui/login",
          element: <LoginPage />
        },
        {
          path: "/ui/projects",
          element: <ProjectList />
        },
        {
          path: "/ui/projects/new",
          element: <ProjectForm />
        },
        {
          path: "/ui/projects/:id",
          element: <ProjectDetailWrapper />
        }
      ]
    }
  ]);
  const provider = <React.StrictMode>
    <RouterProvider router={router} />
  </React.StrictMode>;
  const container = document.getElementById('app');
  const root = createRoot(container!);
  root.render(provider);
}

main();
