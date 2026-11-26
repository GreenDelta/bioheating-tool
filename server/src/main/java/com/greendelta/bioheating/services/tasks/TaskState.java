package com.greendelta.bioheating.services.tasks;

import com.greendelta.bioheating.services.ErrorCode;

/// The state of a task as returned to the client. In case of an error,
/// `errorCode` is the code that is translated in the client and `error`
/// contains technical details.
public record TaskState(
	String id,
	TaskStatus status,
	ErrorCode errorCode,
	String error,
	Object result
) {

	/// The task waits in the queue or is running.
	static TaskState running(String id) {
		return new TaskState(id, TaskStatus.RUNNING, null, null, null);
	}

	/// The task is finished successfully with the given result.
	static TaskState ready(String id, Object result) {
		return new TaskState(id, TaskStatus.READY, null, null, result);
	}

	/// The task failed or was rejected.
	static TaskState error(String id, ErrorCode code, String error) {
		var c = code != null ? code : ErrorCode.TASK_FAILED;
		return new TaskState(id, TaskStatus.ERROR, c, error, null);
	}
}
