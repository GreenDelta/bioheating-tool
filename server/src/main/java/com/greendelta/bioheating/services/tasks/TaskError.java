package com.greendelta.bioheating.services.tasks;

import com.greendelta.bioheating.services.ErrorCode;

/// The error of a failed task.
public record TaskError(
	String id,
	long time,
	long userId,
	ErrorCode code,
	String message
) implements Task {
}
