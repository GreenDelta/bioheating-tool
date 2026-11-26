package com.greendelta.bioheating.services.tasks;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.openlca.commons.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.greendelta.bioheating.model.User;
import com.greendelta.bioheating.services.ErrorCode;

import jakarta.annotation.PreDestroy;

/// Executes long-running tasks like project imports and calculations in the
/// background, with a limited number of parallel tasks and a queue for the
/// waiting tasks. See the README.md for the concepts and configuration.
@Service
public class TaskService {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final Map<String, Entry> store = new ConcurrentHashMap<>();
	private final long taskTimeoutMs;
	private final ThreadPoolExecutor exec;

	public TaskService(
		@Value("${app.tasks.max-parallel}") int maxParallel,
		@Value("${app.tasks.queue-capacity}") int queueCapacity,
		@Value("${app.tasks.retention-minutes}") int retentionMinutes
	) {
		this.taskTimeoutMs = retentionMinutes * 60L * 1000L;
		int n = Math.max(1, maxParallel);
		var threadCount = new AtomicInteger();

		// the same as Executors.newFixedThreadPool(n): n threads that are
		// kept alive; but with a bounded queue so that `execute` throws a
		// RejectedExecutionException when the queue is full; the threads are
		// daemon threads, so that they do not block the shutdown of the JVM
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

	/// Adds the task to the queue and returns its state: `RUNNING` when it was
	/// added, or an `ERROR` when it was rejected because the queue is full.
	public TaskState schedule(Task<?> task) {
		var running = TaskState.running(task.id());
		put(task, running);
		try {
			exec.execute(() -> exec(task));
			return running;
		} catch (RejectedExecutionException e) {
			log.warn("Rejected {} task {}: queue is full", task.type(), task.id());
			var rejected = TaskState.error(
				task.id(), ErrorCode.TOO_MANY_TASKS, "the task queue is full");
			put(task, rejected);
			return rejected;
		}
	}

	private void exec(Task<?> task) {
		long start = System.currentTimeMillis();
		long waitTime = start - task.time();
		log.debug(
			"Started {} task {} after {} ms in the queue; running: {}, queued: {}",
			task.type(), task.id(), waitTime,
			exec.getActiveCount(), exec.getQueue().size());
		boolean ok = false;

		try {
			var res = task.func().get();
			if (res.isError()) {
				put(task, TaskState.error(
					task.id(), task.type().errorCode(), res.error()));
			} else {
				put(task, TaskState.ready(task.id(), res.value()));
				ok = true;
			}

		} catch (Throwable e) {
			// also errors like a StackOverflowError, so that the task does not
			// stay in the running state forever
			log.error("{} task {} failed", task.type(), task.id(), e);
			put(task, TaskState.error(
				task.id(),
				task.type().errorCode(),
				"failed to execute task: " + e.getMessage()));

		} finally {
			log.info(
				"Finished {} task {}: {}; waited {} ms, ran {} ms",
				task.type(), task.id(), ok ? "ok" : "failed",
				waitTime, System.currentTimeMillis() - start);
			if (log.isDebugEnabled()) {
				// log memory in debug mode
				var rt = Runtime.getRuntime();
				long heapMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
				log.debug(
					"Heap used after {} task {}: {} MB",
					task.type(), task.id(), heapMb);
			}
		}
	}

	/// Removes the finished tasks whose results were not fetched by the client
	/// within the retention time.
	@Scheduled(fixedRate = 30, timeUnit = TimeUnit.MINUTES)
	public void runCleanup() {
		long cutoffTime = System.currentTimeMillis() - taskTimeoutMs;
		store.values().removeIf(entry ->
			entry.state().status() != TaskStatus.RUNNING
				&& entry.time() < cutoffTime);
	}

	public Optional<TaskState> getState(User user, String id) {
		if (user == null || Strings.isBlank(id)) return Optional.empty();
		var entry = store.get(id);
		return entry != null && entry.userId() == user.id()
			? Optional.of(entry.state())
			: Optional.empty();
	}

	public boolean remove(User user, String id) {
		if (user == null || Strings.isBlank(id)) return false;
		var entry = store.get(id);
		if (entry == null || entry.userId() != user.id()) return false;
		store.remove(id);
		return true;
	}

	private void put(Task<?> task, TaskState state) {
		var entry = new Entry(task.userId(), System.currentTimeMillis(), state);
		store.put(task.id(), entry);
	}

	/// The current state of a task, together with the user who created the
	/// task and the time of the last state change.
	private record Entry(long userId, long time, TaskState state) {
	}
}
