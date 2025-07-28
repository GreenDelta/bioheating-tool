package com.greendelta.bioheating.services;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.greendelta.bioheating.model.User;
import com.greendelta.bioheating.services.TaskService.Task.Error;
import com.greendelta.bioheating.services.TaskService.Task.NewTask;
import com.greendelta.bioheating.services.TaskService.Task.Result;
import com.greendelta.bioheating.util.Res;
import com.greendelta.bioheating.util.Strings;

@Service
public class TaskService {

	private final Map<String, Task> store = new ConcurrentHashMap<>();
	private final long taskTimeoutMs;

	public TaskService(
		@Value("${bioheating.tasks.retention-minutes:30}") int timeout
	) {
		this.taskTimeoutMs = timeout * 60L * 1000L;
	}

	public void schedule(User user, NewTask<?> task) {
		if (user == null || task == null || Strings.isNil(task.id))
			return;
		if (task.func == null) {
			store.put(task.id, Error.of(task.id, user.id(), "No function provided"));
			return;
		}
		// Update task with user information
		var userTask = new NewTask<>(task.id, task.time, task.func, user.id());
		store.put(task.id, userTask);
		exec(userTask);
	}

	@Async
	private void exec(NewTask<?> task) {
		try {
			var res = task.func.get();
			if (res.hasError()) {
				store.put(task.id, Error.of(task.id, task.userId, res.error()));
			} else {
				store.put(task.id, Result.of(task.id, task.userId, res.value()));
			}
		} catch (Exception e) {
			store.put(task.id, Error.of(
				task.id, task.userId, "failed to execute task: " + e.getMessage()));
		}
	}

	@Async
	@Scheduled(fixedRate = 30, timeUnit = TimeUnit.MINUTES)
	public void runCleanup() {
		long cutoffTime = System.currentTimeMillis() - taskTimeoutMs;
		store.entrySet().removeIf(entry -> {
			Task task = entry.getValue();
			return (task instanceof Error || task instanceof Result) &&
				   task.time() < cutoffTime;
		});
	}

	public Optional<TaskState> getState(User user, String id) {
		if (user == null || Strings.isNil(id))
			return Optional.empty();

		var task = store.get(id);
		if (task == null || !Objects.equals(task.userId(), user.id()))
			return Optional.empty();

		return Optional.of(switch (task) {
			case NewTask<?> ignored -> new TaskState(Status.RUNNING, null, null);
			case Error error -> new TaskState(Status.ERROR, error.message(), null);
			case Result<?> result ->
				new TaskState(Status.READY, null, result.value());
		});
	}

	public boolean deleteTask(User user, String id) {
		if (user == null || Strings.isNil(id))
			return false;

		var task = store.get(id);
		if (task == null || !Objects.equals(task.userId(), user.id()))
			return false;

		store.remove(id);
		return true;
	}

	public record TaskState(Status status, String error, Object result) {
	}

	public enum Status {RUNNING, READY, ERROR}


	public sealed interface Task {

		String id();

		long time();

		Long userId();

		record NewTask<T>(
			String id, long time, Supplier<Res<T>> func, Long userId
		) implements Task {
			public static <T> NewTask<T> of(User user, Supplier<Res<T>> func) {
				var id = UUID.randomUUID().toString();
				long time = System.currentTimeMillis();
				return new NewTask<>(id, time, func, user.id());
			}
		}

		record Error(String id, long time, Long userId, String message) implements Task {

			public static Error of(String id, Long userId, String message) {
				return new Error(id, System.currentTimeMillis(), userId, message);
			}

		}

		record Result<T>(String id, long time, Long userId, T value) implements Task {

			public static <T> Result<T> of(String id, Long userId, T value) {
				return new Result<>(id, System.currentTimeMillis(), userId, value);
			}
		}
	}
}
