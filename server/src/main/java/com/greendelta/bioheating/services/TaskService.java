package com.greendelta.bioheating.services;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.greendelta.bioheating.services.TaskService.Task.Error;
import com.greendelta.bioheating.services.TaskService.Task.NewTask;
import com.greendelta.bioheating.services.TaskService.Task.Result;
import com.greendelta.bioheating.util.Res;
import com.greendelta.bioheating.util.Strings;

@Service
public class TaskService {

	private final Map<String, Task> store = new ConcurrentHashMap<>();

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


	public enum Status {RUNNING, READY, ERROR}


	public sealed interface Task {

		String id();

		long time();

		record NewTask<T>(String id, long time, Supplier<Res<T>> func) implements Task {
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
				return new Result(id, System.currentTimeMillis(), value);
			}

		}
	}

}
