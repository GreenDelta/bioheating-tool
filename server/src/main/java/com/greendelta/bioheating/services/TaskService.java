package com.greendelta.bioheating.services;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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

	public void schedule(NewTask<?> task) {
		if (task == null || Strings.isNil(task.id))
			return;
		if (task.func == null) {
			store.put(task.id, Error.of(task.id, "No function provided"));
			return;
		}
		store.put(task.id, task);
		exec(task);
	}

	@Async
	private void exec(NewTask<?> task) {
		try {
			var res = task.func.get();
			if (res.hasError()) {
				store.put(task.id, Error.of(task.id, res.error()));
			} else {
				store.put(task.id, Result.of(task.id, res.value()));
			}
		} catch (Exception e) {
			store.put(task.id, Error.of(
				task.id, "failed to execute task: " + e.getMessage()));
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

	public TaskState getState(String id) {
		if (Strings.isNil(id))
			return new TaskState(Status.ERROR, "No task ID provided", null);

		var task = store.get(id);
		if (task == null)
			return new TaskState(Status.ERROR, "Task not found", null);

		return switch (task) {
			case NewTask<?> ignored -> new TaskState(Status.RUNNING, null, null);
			case Error error -> new TaskState(Status.ERROR, error.message(), null);
			case Result<?> result ->
				new TaskState(Status.READY, null, result.value());
		};
	}

	public record TaskState(Status status, String error, Object result) {
	}

	public enum Status {RUNNING, READY, ERROR}


	public sealed interface Task {

		String id();

		long time();

		record NewTask<T>(
			String id, long time, Supplier<Res<T>> func
		) implements Task {
			public static <T> NewTask<T> of(Supplier<Res<T>> func) {
				var id = UUID.randomUUID().toString();
				long time = System.currentTimeMillis();
				return new NewTask<>(id, time, func);
			}
		}

		record Error(String id, long time, String message) implements Task {

			public static Error of(String id, String message) {
				return new Error(id, System.currentTimeMillis(), message);
			}

		}

		record Result<T>(String id, long time, T value) implements Task {

			public static <T> Result<T> of(String id, T value) {
				return new Result<>(id, System.currentTimeMillis(), value);
			}
		}
	}
}
