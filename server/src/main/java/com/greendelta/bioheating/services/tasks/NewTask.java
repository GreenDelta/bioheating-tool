package com.greendelta.bioheating.services.tasks;

import java.util.UUID;
import java.util.function.Supplier;

import org.openlca.commons.Res;

import com.greendelta.bioheating.model.User;

/// A task that waits in the queue or is running. `time` is the time when the
/// task was created.
public record NewTask<T>(
	String id,
	long time,
	long userId,
	TaskType type,
	Supplier<Res<T>> func
) implements Task {

	public static <T> NewTask<T> of(
		User user,
		TaskType type,
		Supplier<Res<T>> func
	) {
		var id = UUID.randomUUID().toString();
		long time = System.currentTimeMillis();
		return new NewTask<>(id, time, user.id(), type, func);
	}
}
