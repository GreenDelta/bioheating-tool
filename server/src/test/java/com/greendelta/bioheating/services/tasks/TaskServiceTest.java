package com.greendelta.bioheating.services.tasks;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openlca.commons.Res;

import com.greendelta.bioheating.model.User;
import com.greendelta.bioheating.services.ErrorCode;

class TaskServiceTest {

	private User user;
	private TaskService service;

	@BeforeEach
	void setUp() {
		user = new User();
		user.id(42);
	}

	@AfterEach
	void tearDown() {
		if (service != null) {
			service.shutdown();
		}
	}

	/// With max. 2 parallel tasks, 2 tasks run at the same time and a third
	/// task waits in the queue until one of them is finished.
	@Test
	void testMaxParallel() throws Exception {
		service = new TaskService(2, 10, 30);

		// the first two tasks count down `twoTasksStarted` and then block until
		// we count down `finishTasks`; the third task counts down
		// `thirdTaskStarted` and finishes directly
		var twoTasksStarted = new CountDownLatch(2);
		var thirdTaskStarted = new CountDownLatch(1);
		var finishTasks = new CountDownLatch(1);

		Supplier<Res<String>> blockingTask = () -> {
			twoTasksStarted.countDown();
			waitFor(finishTasks);
			return Res.ok("done");
		};
		service.schedule(Task.of(user, TaskType.CALCULATION, blockingTask));
		service.schedule(Task.of(user, TaskType.CALCULATION, blockingTask));
		service.schedule(Task.of(user, TaskType.CALCULATION, () -> {
			thirdTaskStarted.countDown();
			return Res.ok("done");
		}));

		// both blocking tasks are running at the same time
		assertTrue(twoTasksStarted.await(5, TimeUnit.SECONDS));
		// ... and the third task has to wait
		assertFalse(thirdTaskStarted.await(200, TimeUnit.MILLISECONDS));

		// when the blocking tasks are finished, the third task can start
		finishTasks.countDown();
		assertTrue(thirdTaskStarted.await(5, TimeUnit.SECONDS));
	}

	/// With max. 1 parallel task and a queue capacity of 1, a third task is
	/// rejected: the first task runs, the second waits in the queue.
	@Test
	void testQueueFull() {
		service = new TaskService(1, 1, 30);

		var finishTasks = new CountDownLatch(1);
		Supplier<Res<String>> blockingTask = () -> {
			waitFor(finishTasks);
			return Res.ok("done");
		};

		var first = Task.of(user, TaskType.CALCULATION, blockingTask);
		var second = Task.of(user, TaskType.CALCULATION, blockingTask);
		var third = Task.of(user, TaskType.CALCULATION, blockingTask);
		assertEquals(TaskStatus.RUNNING, service.schedule(first).status());
		assertEquals(TaskStatus.RUNNING, service.schedule(second).status());

		// the rejection is directly returned by `schedule`
		var state = service.schedule(third);
		assertEquals(TaskStatus.ERROR, state.status());
		assertEquals(ErrorCode.TOO_MANY_TASKS, state.errorCode());

		// ... and is also stored for the client
		var stored = service.getState(user, third.id()).orElseThrow();
		assertEquals(ErrorCode.TOO_MANY_TASKS, stored.errorCode());

		finishTasks.countDown();
	}

	/// The result of a successful task is returned to the client.
	@Test
	void testResult() throws Exception {
		service = new TaskService(1, 10, 30);
		var task = Task.of(user, TaskType.CALCULATION, () -> Res.ok("done"));
		service.schedule(task);

		var state = waitUntilDone(task);
		assertEquals(TaskStatus.READY, state.status());
		assertEquals("done", state.result());
	}

	/// When a task fails, the client gets the error code of the task type.
	@Test
	void testError() throws Exception {
		service = new TaskService(1, 10, 30);
		var task = Task.of(
			user, TaskType.PROJECT_UPDATE, () -> Res.error("failed"));
		service.schedule(task);

		var state = waitUntilDone(task);
		assertEquals(TaskStatus.ERROR, state.status());
		assertEquals(ErrorCode.PROJECT_UPDATE_FAILED, state.errorCode());
	}

	/// Polls the state of the task until it is not running anymore, like the
	/// client does.
	private TaskState waitUntilDone(Task<?> task) throws Exception {
		long end = System.currentTimeMillis() + 5000;
		while (System.currentTimeMillis() < end) {
			var state = service.getState(user, task.id()).orElseThrow();
			if (state.status() != TaskStatus.RUNNING)
				return state;
			Thread.sleep(10);
		}
		throw new AssertionError("task did not finish: " + task.id());
	}

	/// Waits for the latch inside a task; the task function cannot throw the
	/// checked `InterruptedException`.
	private static void waitFor(CountDownLatch latch) {
		try {
			latch.await(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
