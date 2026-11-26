package com.greendelta.bioheating.services.tasks;

import com.greendelta.bioheating.services.ErrorCode;

/// The types of tasks with the error codes that are reported to the client
/// when a task fails.
public enum TaskType {

	PROJECT_IMPORT(ErrorCode.PROJECT_IMPORT_FAILED),
	PROJECT_UPDATE(ErrorCode.PROJECT_UPDATE_FAILED),
	CALCULATION(ErrorCode.CALCULATION_FAILED);

	private final ErrorCode errorCode;

	TaskType(ErrorCode errorCode) {
		this.errorCode = errorCode;
	}

	public ErrorCode errorCode() {
		return errorCode;
	}
}
