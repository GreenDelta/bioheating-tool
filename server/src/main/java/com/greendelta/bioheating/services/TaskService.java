package com.greendelta.bioheating.services;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Qualifier;
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
	private final Executor exec;

	public TaskService(
		@Value("${bioheating.tasks.retention-minutes:30}") int timeout,
		@Qualifier("applicationTaskExecutor") Executor exec
	) {
		this.taskTimeoutMs = timeout * 60L * 1000L;
		this.exec = exec;
	}

	public void schedule(NewTask<?> task) {
		if (task == null || Strings.isNil(task.id))
			return;
		if (task.func == null) {
			store.put(task.id, task.toError("No function provided"));
			return;
		}
		store.put(task.id, task);
		CompletableFuture.runAsync(() -> exec(task), exec);
	}

	private void exec(NewTask<?> task) {
		try {
			var res = task.func.get();
			if (res.hasError()) {
				store.put(task.id, task.toError(res.error()));
			} else {
				store.put(task.id, task.toResult(res.value()));
			}
		} catch (Exception e) {
			store.put(task.id,
				task.toError("failed to execute task: " + e.getMessage()));
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
		return task != null && task.userId() == user.id()
			? Optional.of(task.toState())
			: Optional.empty();
	}

	public boolean remove(User user, String id) {
		if (user == null || Strings.isNil(id))
			return false;
		var task = store.get(id);
		if (task == null || task.userId() != user.id())
			return false;
		store.remove(id);
		return true;
	}

	public record TaskState(
		String id, Status status, String error, Object result
	) {
	}

	public enum Status {RUNNING, READY, ERROR}


	public sealed interface Task {

		String id();

		long time();

		long userId();

		default TaskState toState() {
			return switch (this) {
				case NewTask<?> ignored ->
					new TaskState(id(), Status.RUNNING, null, null);
				case Error error -> new
					TaskState(id(), Status.ERROR, error.message(), null);
				case Result<?> result ->
					new TaskState(id(), Status.READY, null, result.value());
			};
		}

		default Error toError(String message) {
			return new Error(id(), System.currentTimeMillis(), userId(), message);
		}

		default <T> Result<T> toResult(T value) {
			return new Result<>(id(), System.currentTimeMillis(), userId(), value);
		}

		record NewTask<T>(
			String id, long time, long userId, Supplier<Res<T>> func
		) implements Task {

			public static <T> NewTask<T> of(User user, Supplier<Res<T>> func) {
				var id = UUID.randomUUID().toString();
				long time = System.currentTimeMillis();
				return new NewTask<>(id, time, user.id(), func);
			}
		}

		record Error(
			String id, long time, long userId, String message
		) implements Task {
		}

		record Result<T>(
			String id, long time, long userId, T value
		) implements Task {
		}
	}
}
