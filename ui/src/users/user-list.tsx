import React from "react";
import {
	Link,
	useLoaderData,
	useNavigate,
	useOutletContext,
} from "react-router-dom";
import { User } from "../model";
import { AddIcon, DeleteIcon } from "../components/icons";
import * as api from "../api";

export const UserList = () => {
	const navigate = useNavigate();
	const [currentUser] = useOutletContext<[User]>();
	const users: User[] = useLoaderData();
	const [deletable, setDeletable] = React.useState<User | null>(null);

	const onDelete = async (b: boolean) => {
		if (!b || !deletable) {
			setDeletable(null);
			return;
		}

		const u = deletable;
		const res = await api.deleteUser(u.id);

		if (res.isErr) {
			const params = new URLSearchParams({
				message: "Failed to delete user",
				details: res.error,
			});
			navigate(`/ui/error?${params.toString()}`);
		} else {
			// Remove user from the local list on successful deletion
			const idx = users.indexOf(u);
			if (idx > -1) {
				users.splice(idx, 1);
			}
			// Refresh the page to update the list
			window.location.reload();
		}

		setDeletable(null);
	};

	return (
		<div>
			<nav aria-label="breadcrumb">
				<ol className="breadcrumb">
					<li className="breadcrumb-item">
						<Link to="/">Home</Link>
					</li>
					<li className="breadcrumb-item active" aria-current="page">
						Users
					</li>
				</ol>
			</nav>
			<DeleteDialog user={deletable} doIt={onDelete} />
			<UserTable
				users={users}
				onDelete={setDeletable}
				currentUser={currentUser}
			/>
		</div>
	);
};

const UserTable = ({
	users,
	onDelete,
	currentUser,
}: {
	users: User[];
	onDelete: (u: User) => void;
	currentUser: User;
}) => {
	const navigate = useNavigate();

	if (!users || users.length === 0) {
		return (
			<div className="text-center">
				<p>No users found.</p>
				<button
					className="btn btn-primary"
					onClick={() => navigate("/ui/users/new")}>
					Create first user
				</button>
			</div>
		);
	}

	const rows = users.map(u => (
		<tr key={u.id}>
			<td>
				<strong>{u.name}</strong>
			</td>
			<td>{u.fullName}</td>
			<td>
				{u.isAdmin ? (
					<span className="badge bg-primary">Admin</span>
				) : (
					<span className="badge bg-secondary">User</span>
				)}
			</td>
			<td className="text-end">
				{u.id !== currentUser.id && (
					<DeleteIcon
						color="#dc3545"
						onClick={() => onDelete(u)}
						tooltip="Delete user"
					/>
				)}
			</td>
		</tr>
	));

	return (
		<div>
			<table className="table table-hover">
				<thead>
					<tr>
						<th>Username</th>
						<th>Full Name</th>
						<th>Role</th>
						<th className="text-end">Actions</th>
					</tr>
				</thead>
				<tbody>
					{rows}
					<tr>
						<td colSpan={3}></td>
						<td className="text-end">
							<AddIcon
								tooltip="Create a new user"
								onClick={() => navigate("/ui/users/new")}
							/>
						</td>
					</tr>
				</tbody>
			</table>
		</div>
	);
};

const DeleteDialog = ({
	user,
	doIt,
}: {
	user: User | null;
	doIt: (b: boolean) => void;
}) => {
	if (!user) {
		return <></>;
	}
	return (
		<div
			className="modal d-block"
			style={{ backgroundColor: "rgba(0,0,0,0.5)" }}>
			<div className="modal-dialog">
				<div className="modal-content">
					<div className="modal-header">
						<h5 className="modal-title">Delete user?</h5>
					</div>
					<div className="modal-body">
						<p>
							Do you really want to delete user <strong>{user.name}</strong> (
							{user.fullName})? Note that this will delete all user data and
							cannot be undone.
						</p>
					</div>
					<div className="modal-footer">
						<button
							type="button"
							className="btn btn-secondary"
							onClick={() => doIt(false)}>
							Cancel
						</button>
						<button
							type="button"
							className="btn btn-danger"
							onClick={() => doIt(true)}>
							Delete
						</button>
					</div>
				</div>
			</div>
		</div>
	);
};
