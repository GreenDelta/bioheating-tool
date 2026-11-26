package com.greendelta.bioheating.services.tasks;

import com.greendelta.bioheating.services.ErrorCode;

/// A task in the store of the `TaskService`. A task starts as a `NewTask`
/// while it waits in the queue or runs, and is then replaced by a
/// `TaskResult` or `TaskError` when it is finished.
public sealed interface Task permits NewTask, TaskResult, TaskError {

	String id();

	/// The time when the task was created or, for results and errors, when
	/// the task was finished.
	long time();

	long userId();

	default TaskState toState() {
		return switch (this) {
			case NewTask<?> ignored -> new TaskState(
				id(),
				TaskStatus.RUNNING,
				null,
				null,
				null
			);
			case TaskError error -> new TaskState(
				id(),
				TaskStatus.ERROR,
				error.code(),
				error.message(),
				null
			);
			case TaskResult<?> result -> new TaskState(
				id(),
				TaskStatus.READY,
				null,
				null,
				result.value()
			);
		};
	}

	default TaskError toError(ErrorCode code, String message) {
		return new TaskError(
			id(),
			System.currentTimeMillis(),
			userId(),
			code != null ? code : ErrorCode.TASK_FAILED,
			message
		);
	}

	default <T> TaskResult<T> toResult(T value) {
		return new TaskResult<>(id(), System.currentTimeMillis(), userId(), value);
	}
}
