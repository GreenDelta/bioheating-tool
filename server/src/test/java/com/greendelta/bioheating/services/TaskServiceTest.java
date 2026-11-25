package com.greendelta.bioheating.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.greendelta.bioheating.model.User;
import com.greendelta.bioheating.services.TaskService.Status;
import com.greendelta.bioheating.services.TaskService.Task.NewTask;
import com.greendelta.bioheating.services.TaskService.TaskType;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openlca.commons.Res;

class TaskServiceTest {

	private final User user = user();
	private final TaskService service = new TaskService(1, 1, 30);

	@AfterEach
	void shutdown() {
		service.shutdown();
	}

	@Test
	void limitsParallelTasksAndRejectsWhenTheQueueIsFull() throws Exception {
		var started = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var running = new AtomicInteger();
		var maxRunning = new AtomicInteger();

		var first = NewTask.of(user, TaskType.CALCULATION, () -> {
			track(running, maxRunning);
			started.countDown();
			await(release);
			running.decrementAndGet();
			return Res.ok("first");
		});
		var second = NewTask.of(user, TaskType.CALCULATION, () -> {
			track(running, maxRunning);
			running.decrementAndGet();
			return Res.ok("second");
		});
		var third = NewTask.of(user, TaskType.CALCULATION, () -> Res.ok("third"));

		service.schedule(first);
		assertTrue(started.await(5, TimeUnit.SECONDS));
		service.schedule(second); // waits in the queue
		service.schedule(third); // the queue is full

		var rejected = service.getState(user, third.id()).orElseThrow();
		assertEquals(Status.ERROR, rejected.status());
		assertEquals(ErrorCode.TOO_MANY_TASKS, rejected.errorCode());
		assertEquals(Status.RUNNING, stateOf(second.id()));

		release.countDown();
		awaitReady(first.id());
		awaitReady(second.id());
		assertEquals(1, maxRunning.get());
	}

	@Test
	void reportsTheErrorCodeOfTheTaskType() throws Exception {
		var task = NewTask.of(
			user, TaskType.PROJECT_UPDATE, () -> Res.error("failed"));
		service.schedule(task);
		var state = awaitDone(task.id());
		assertEquals(Status.ERROR, state.status());
		assertEquals(ErrorCode.PROJECT_UPDATE_FAILED, state.errorCode());
	}

	private Status stateOf(String id) {
		return service.getState(user, id).orElseThrow().status();
	}

	private void awaitReady(String id) throws Exception {
		assertEquals(Status.READY, awaitDone(id).status());
	}

	private TaskService.TaskState awaitDone(String id) throws Exception {
		long end = System.currentTimeMillis() + 5000;
		while (System.currentTimeMillis() < end) {
			var state = service.getState(user, id).orElseThrow();
			if (state.status() != Status.RUNNING) return state;
			Thread.sleep(10);
		}
		throw new AssertionError("task did not finish: " + id);
	}

	private static void track(AtomicInteger running, AtomicInteger max) {
		max.accumulateAndGet(running.incrementAndGet(), Math::max);
	}

	private static void await(CountDownLatch latch) {
		try {
			latch.await(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private static User user() {
		var user = new User();
		user.id(42);
		return user;
	}
}
