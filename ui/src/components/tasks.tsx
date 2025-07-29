import React, { useEffect, useRef, useState } from "react";
import * as api from "../api";
import { TaskState, TaskStatus } from "../model";

interface Props {
	taskId: string;
	message: string;
	onSuccess: (result: any) => void;
}

export const TaskPanel = ({ taskId, message, onSuccess }: Props) => {
	const [state, setState] = useState<TaskState | null>(null);
	const [error, setError] = useState<string | null>(null);
	const timeoutRef = useRef<any>(null);

	useEffect(() => {
		const poll = async () => {
			try {
				const res = await api.getTaskState(taskId);
				if (res.isErr) {
					setError(res.error);
					return;
				}

				const s = res.value;
				if (s.status === TaskStatus.RUNNING) {
					timeoutRef.current = setTimeout(poll, 1000);
					return;
				}

				await api.dropTaskResult(taskId);
				setState(s);
			} catch (err) {
				setError(`Failed to poll task status: ${err}`);
				if (timeoutRef.current) {
					clearTimeout(timeoutRef.current);
				}
			}
		};

		poll();

		return () => {
			if (timeoutRef.current) {
				clearTimeout(timeoutRef.current);
			}
		};

	}, [taskId]);

	if (!state || !state.status) {
		return <ProgressPanel message={message} />;
	}

	if (error || state.status === TaskStatus.ERROR) {
		return <ErrorPanel message={error || state.error || "unknown error"} />;
	}

	if (state.status === TaskStatus.READY) {
		onSuccess(state.result);
		return null;
	}

	return <ErrorPanel message="Unknown task state" />;
};

const ProgressPanel = ({ message }: { message: string }) => (
	<div className="d-flex align-items-center">
		<span>{message}</span>
		<div className="progress mt-3">
			<div
				className="progress-bar progress-bar-striped progress-bar-animated"
				role="progressbar"
				style={{ width: "100%" }}
			/>
		</div>
	</div>
);

const ErrorPanel = ({ message }: { message: string }) => (
	<div className="d-flex align-items-center">
		<div className="alert alert-danger" role="alert">
			<h5 className="alert-heading">Task failed</h5>
			<p>{message}</p>
		</div>
	</div>
);
