package com.greendelta.bioheating.services.tasks;

import java.util.UUID;
import java.util.function.Supplier;

import org.openlca.commons.Res;

import com.greendelta.bioheating.model.User;

/// A task that is executed in the background by the `TaskService`. `func` is
/// the work of the task, and `time` is the time when the task was created.
public record Task<T>(
	String id,
	long time,
	long userId,
	TaskType type,
	Supplier<Res<T>> func
) {

	public static <T> Task<T> of(
		User user,
		TaskType type,
		Supplier<Res<T>> func
	) {
		var id = UUID.randomUUID().toString();
		long time = System.currentTimeMillis();
		return new Task<>(id, time, user.id(), type, func);
	}
}
