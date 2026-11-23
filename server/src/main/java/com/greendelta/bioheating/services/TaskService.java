package com.greendelta.bioheating.services;

import com.greendelta.bioheating.model.User;
import com.greendelta.bioheating.services.TaskService.Task.Error;
import com.greendelta.bioheating.services.TaskService.Task.NewTask;
import com.greendelta.bioheating.services.TaskService.Task.Result;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.openlca.commons.Res;
import org.openlca.commons.Strings;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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
		if (task == null || Strings.isBlank(task.id)) return;
		if (task.func == null) {
			store.put(
				task.id,
				task.toError(ErrorCode.TASK_FAILED, "No function provided")
			);
			return;
		}
		store.put(task.id, task);
		CompletableFuture.runAsync(() -> exec(task), exec);
	}

	private void exec(NewTask<?> task) {
		try {
			var res = task.func.get();
			if (res.isError()) {
				store.put(task.id, task.toError(task.errorCode, res.error()));
			} else {
				store.put(task.id, task.toResult(res.value()));
			}
		} catch (Exception e) {
			store.put(
				task.id,
				task.toError(
					task.errorCode,
					"failed to execute task: " + e.getMessage()
				)
			);
		}
	}

	@Async
	@Scheduled(fixedRate = 30, timeUnit = TimeUnit.MINUTES)
	public void runCleanup() {
		long cutoffTime = System.currentTimeMillis() - taskTimeoutMs;
		store
			.entrySet()
			.removeIf(entry -> {
				Task task = entry.getValue();
				return (
					(task instanceof Error || task instanceof Result) &&
					task.time() < cutoffTime
				);
			});
	}

	public Optional<TaskState> getState(User user, String id) {
		if (user == null || Strings.isBlank(id)) return Optional.empty();
		var task = store.get(id);
		return task != null && task.userId() == user.id()
			? Optional.of(task.toState())
			: Optional.empty();
	}

	public boolean remove(User user, String id) {
		if (user == null || Strings.isBlank(id)) return false;
		var task = store.get(id);
		if (task == null || task.userId() != user.id()) return false;
		store.remove(id);
		return true;
	}

	/// The state of a task as returned to the client. In case of an error,
	/// `errorCode` is the code that is translated in the client and `error`
	/// contains technical details.
	public record TaskState(
		String id,
		Status status,
		ErrorCode errorCode,
		String error,
		Object result
	) {}

	public enum Status {
		RUNNING,
		READY,
		ERROR,
	}

	public sealed interface Task {
		String id();

		long time();

		long userId();

		default TaskState toState() {
			return switch (this) {
				case NewTask<?> ignored -> new TaskState(
					id(),
					Status.RUNNING,
					null,
					null,
					null
				);
				case Error error -> new TaskState(
					id(),
					Status.ERROR,
					error.code(),
					error.message(),
					null
				);
				case Result<?> result -> new TaskState(
					id(),
					Status.READY,
					null,
					null,
					result.value()
				);
			};
		}

		default Error toError(ErrorCode code, String message) {
			return new Error(
				id(),
				System.currentTimeMillis(),
				userId(),
				code != null ? code : ErrorCode.TASK_FAILED,
				message
			);
		}

		default <T> Result<T> toResult(T value) {
			return new Result<>(id(), System.currentTimeMillis(), userId(), value);
		}

		/// A new task. `errorCode` is the code that is reported to the client
		/// when the task fails.
		record NewTask<T>(
			String id,
			long time,
			long userId,
			ErrorCode errorCode,
			Supplier<Res<T>> func
		) implements Task {
			public static <T> NewTask<T> of(
				User user,
				ErrorCode errorCode,
				Supplier<Res<T>> func
			) {
				var id = UUID.randomUUID().toString();
				long time = System.currentTimeMillis();
				return new NewTask<>(id, time, user.id(), errorCode, func);
			}
		}

		record Error(
			String id,
			long time,
			long userId,
			ErrorCode code,
			String message
		) implements Task {}

		record Result<T>(
			String id,
			long time,
			long userId,
			T value
		) implements Task {}
	}
}
