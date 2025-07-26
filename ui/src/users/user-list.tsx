import React from "react";
import { Link, useLoaderData, useNavigate, useOutletContext } from "react-router-dom";
import { User } from "../model";
import { AddIcon, DeleteIcon } from "../icons";
import * as api from "../api";

export const UserList = () => {
	const navigate = useNavigate();
	const [currentUser] = useOutletContext<[User]>();
	const users: User[] = useLoaderData();
	const [deletable, setDeletable] = React.useState<User | null>(null);

	// Only admins should see this component
	if (!currentUser.isAdmin) {
		return (
			<div className="alert alert-danger">
				<h4>Access Denied</h4>
				<p>Only administrators can access the user management.</p>
			</div>
		);
	}

	const onDelete = (b: boolean) => {
		if (!b || !deletable) {
			setDeletable(null);
			return;
		}

		const u = deletable;
		const idx = users.indexOf(u);
		if (idx > -1) {
			users.splice(idx, 1);
		}

		// Note: We'd need a deleteUser API method in the backend for this to work
		// For now, just show an error message
		const params = new URLSearchParams({
			message: "Delete user not implemented",
			details: "User deletion functionality is not yet implemented in the backend.",
		});
		navigate(`/ui/error?${params.toString()}`);
		setDeletable(null);
	};

	return (
		<div>
			<h1>User Management</h1>
			<DeleteDialog user={deletable} doIt={onDelete} />
			<UserTable users={users} onDelete={setDeletable} currentUser={currentUser} />
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
	if (!users || users.length === 0) {
		return (
			<div className="text-center">
				<p>No users found.</p>
				<button className="btn btn-primary" disabled>
					Create new user
				</button>
				<small className="d-block text-muted mt-2">
					User creation will be available in a future update.
				</small>
			</div>
		);
	}

	const rows = users.map(u => (
		<tr key={u.name}>
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
				{u.name !== currentUser.name && (
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
								tooltip="Create a new user (coming soon)"
								onClick={() => {
									// TODO: Navigate to user creation form when implemented
									alert("User creation will be available in a future update.");
								}}
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
							Do you really want to delete user{" "}
							<strong>{user.name}</strong> ({user.fullName})?
							Note that this cannot be undone.
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
