import React from "react";
import { useLoaderData, useNavigate, useOutletContext } from "react-router-dom";
import { Trans, useTranslation } from "react-i18next";
import { User } from "../model";
import errors from "../components/errors";
import { AddIcon, DeleteIcon, EditIcon } from "../components/icons";
import * as api from "../api";
import { FluidBreadcrumbRow } from "../components/navi";

export const UserList = () => {
	const { t } = useTranslation();
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
			errors.navigate(navigate, "loadErrors.deleteUser", res);
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
			<div className="container-fluid">
				<FluidBreadcrumbRow
					active={t("users.title")}
					path={[["/", t("common.home")]]}
				/>
				<div className="row justify-content-center">
					<div className="col-12 col-md-10 col-lg-8">
						<UserTable
							users={users}
							onDelete={setDeletable}
							currentUser={currentUser}
						/>
					</div>
				</div>
			</div>
			<DeleteDialog user={deletable} doIt={onDelete} />
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
	const { t } = useTranslation();

	if (!users || users.length === 0) {
		return (
			<div className="text-center">
				<p>{t("users.noUsers")}</p>
				<button
					className="btn btn-primary"
					onClick={() => navigate("/ui/users/new")}>
					{t("users.createFirst")}
				</button>
			</div>
		);
	}

	const rows = users.map(u => (
		<tr key={u.id}>
			<td>{u.name}</td>
			<td>{u.fullName}</td>
			<td>
				{u.isAdmin ? (
					<span className="badge bg-primary">{t("users.admin")}</span>
				) : (
					<span className="badge bg-secondary">{t("users.user")}</span>
				)}
			</td>
			<td className="text-end">
				<div className="d-flex gap-2 justify-content-end">
					<EditIcon
						onClick={() => navigate(`/ui/users/${u.id}/edit`)}
						tooltip={t("users.edit")}
					/>
					{u.id !== currentUser.id && (
						<DeleteIcon
							color="#dc3545"
							onClick={() => onDelete(u)}
							tooltip={t("users.delete")}
						/>
					)}
				</div>
			</td>
		</tr>
	));

	return (
		<div>
			<table className="table table-hover">
				<thead>
					<tr>
						<th>{t("users.user")}</th>
						<th>{t("users.fullName")}</th>
						<th>{t("users.role")}</th>
						<th className="text-end">{t("common.actions")}</th>
					</tr>
				</thead>
				<tbody>
					{rows}
					<tr>
						<td colSpan={3}></td>
						<td className="text-end">
							<AddIcon
								tooltip={t("users.createNew")}
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
	const { t } = useTranslation();
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
						<h5 className="modal-title">{t("users.deleteTitle")}</h5>
					</div>
					<div className="modal-body">
						<p>
							<Trans
								i18nKey="users.deleteConfirm"
								values={{ name: user.name, fullName: user.fullName }}
								components={{ 1: <strong /> }}
							/>
						</p>
					</div>
					<div className="modal-footer">
						<button
							type="button"
							className="btn btn-secondary"
							onClick={() => doIt(false)}>
							{t("common.cancel")}
						</button>
						<button
							type="button"
							className="btn btn-danger"
							onClick={() => doIt(true)}>
							{t("common.delete")}
						</button>
					</div>
				</div>
			</div>
		</div>
	);
};
