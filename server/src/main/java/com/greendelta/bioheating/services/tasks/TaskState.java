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
}
