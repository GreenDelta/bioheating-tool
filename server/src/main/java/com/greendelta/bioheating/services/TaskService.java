package com.greendelta.bioheating.services;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.openlca.commons.Res;
import org.openlca.commons.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.greendelta.bioheating.model.User;
import com.greendelta.bioheating.services.TaskService.Task.Error;
import com.greendelta.bioheating.services.TaskService.Task.NewTask;
import com.greendelta.bioheating.services.TaskService.Task.Result;

import jakarta.annotation.PreDestroy;

/// Executes long-running tasks like project imports and calculations in the
/// background. At most `app.tasks.max-parallel` tasks run at the same
/// time; further tasks wait in a queue with the capacity
/// `app.tasks.queue-capacity`. When the queue is full, new tasks are
/// rejected. The results of finished tasks are kept for
/// `app.tasks.retention-minutes` until the client fetches them.
@Service
public class TaskService {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final Map<String, Task> store = new ConcurrentHashMap<>();
	private final long taskTimeoutMs;
	private final ThreadPoolExecutor exec;

	public TaskService(
		@Value("${app.tasks.max-parallel:2}") int maxParallel,
		@Value("${app.tasks.queue-capacity:100}") int queueCapacity,
		@Value("${app.tasks.retention-minutes:30}") int retentionMinutes
	) {
		this.taskTimeoutMs = retentionMinutes * 60L * 1000L;
		int n = Math.max(1, maxParallel);
		var threadCount = new AtomicInteger();
		this.exec = new ThreadPoolExecutor(
			n, n, 0L, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<>(Math.max(1, queueCapacity)),
			r -> {
				var thread = new Thread(r, "task-" + threadCount.incrementAndGet());
				thread.setDaemon(true);
				return thread;
			});
		log.info(
			"Task execution: max. {} parallel tasks, queue capacity {}",
			n, queueCapacity);
	}

	@PreDestroy
	void shutdown() {
		exec.shutdownNow();
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
		try {
			exec.execute(() -> exec(task));
		} catch (RejectedExecutionException e) {
			log.warn("Rejected {} task {}: queue is full", task.type, task.id);
			store.put(
				task.id,
				task.toError(ErrorCode.TOO_MANY_TASKS, "the task queue is full")
			);
		}
	}

	private void exec(NewTask<?> task) {
		long start = System.currentTimeMillis();
		log.info(
			"Started {} task {} after {} ms in the queue; running: {}, queued: {}",
			task.type, task.id, start - task.time,
			exec.getActiveCount(), exec.getQueue().size());
		boolean ok = false;
		try {
			var res = task.func.get();
			if (res.isError()) {
				store.put(task.id, task.toError(task.type.errorCode(), res.error()));
			} else {
				store.put(task.id, task.toResult(res.value()));
				ok = true;
			}
		} catch (Throwable e) {
			// also errors like a StackOverflowError, so that the task does not
			// stay in the running state forever
			log.error("{} task {} failed", task.type, task.id, e);
			store.put(
				task.id,
				task.toError(
					task.type.errorCode(),
					"failed to execute task: " + e.getMessage()
				)
			);
		} finally {
			var rt = Runtime.getRuntime();
			long heapMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
			log.info(
				"Finished {} task {} in {} ms: {}; heap used: {} MB",
				task.type, task.id, System.currentTimeMillis() - start,
				ok ? "ok" : "failed", heapMb);
		}
	}

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

		/// A new task. `time` is the time when the task was created.
		record NewTask<T>(
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
