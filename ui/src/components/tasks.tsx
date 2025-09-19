import React, { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import * as api from "../api";
import { TaskState, TaskStatus } from "../model";

interface Props {
	taskId: string;
	message: string;
	getTargetUrl: (result: any) => string;
}

export const TaskPanel = ({ taskId, message, getTargetUrl }: Props) => {
	const [state, setState] = useState<TaskState | null>(null);
	const [error, setError] = useState<string | null>(null);
	const timeoutRef = useRef<any>(null);
	const navigate = useNavigate();

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

	useEffect(() => {
		if (state && state.status === TaskStatus.READY) {
			const targetUrl = getTargetUrl(state.result);
			navigate(targetUrl);
		}
	}, [state]);

	if (!state || !state.status) {
		return <ProgressPanel message={message} />;
	}

	if (error || state.status === TaskStatus.ERROR) {
		return <ErrorPanel message={error || state.error || "unknown error"} />;
	}

	if (state.status === TaskStatus.READY) {
		return <ProgressPanel message="Task completed successfully..." />;
	}

	return <ErrorPanel message="Unknown task state" />;
};

const ProgressPanel = ({ message }: { message: string }) => (
	<div className="d-flex justify-content-center">
		<div className="card text-center" style={{ maxWidth: "600px" }}>
			<div className="card-body py-5">
				<div className="mb-4">
					<div className="spinner-border text-primary" role="status" style={{ width: "3rem", height: "3rem" }}>
						<span className="visually-hidden">Loading...</span>
					</div>
				</div>
				<h6 className="card-title text-muted mb-0">{message}</h6>
			</div>
		</div>
	</div>
);

const ErrorPanel = ({ message }: { message: string }) => (
	<div className="text-center py-4">
		<div className="alert alert-danger" role="alert">
			<h5 className="alert-heading mb-3">
				<i className="bi bi-exclamation-triangle-fill me-2"></i>
				Task Failed
			</h5>
			<p className="mb-0">{message}</p>
		</div>
	</div>
);
