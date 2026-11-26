package com.greendelta.bioheating.services.tasks;

/// The result of a successfully finished task.
public record TaskResult<T>(
	String id,
	long time,
	long userId,
	T value
) implements Task {
}
