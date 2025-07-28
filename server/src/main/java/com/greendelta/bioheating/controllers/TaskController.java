package com.greendelta.bioheating.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greendelta.bioheating.services.TaskService;
import com.greendelta.bioheating.services.TaskService.TaskState;
import com.greendelta.bioheating.services.UserService;
import com.greendelta.bioheating.util.Http;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

	private final TaskService taskService;
	private final UserService userService;

	public TaskController(TaskService taskService, UserService userService) {
		this.taskService = taskService;
		this.userService = userService;
	}

	@GetMapping("/{id}")
	public ResponseEntity<?> getTaskState(Authentication auth, @PathVariable String id) {
		var user = userService.getCurrentUser(auth).orElse(null);
		if (user == null)
			return Http.badRequest("not authenticated");

		var state = taskService.getState(user, id);
		return state.isPresent()
			? Http.ok(state.get())
			: Http.notFound("task not found: " + id);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteTask(Authentication auth, @PathVariable String id) {
		var user = userService.getCurrentUser(auth).orElse(null);
		if (user == null)
			return Http.badRequest("not authenticated");

		boolean deleted = taskService.deleteTask(user, id);
		return deleted
			? Http.ok("task deleted successfully")
			: Http.notFound("task not found: " + id);
	}
}
