package com.greendelta.bioheating.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greendelta.bioheating.services.TaskService;
import com.greendelta.bioheating.services.TaskService.TaskState;
import com.greendelta.bioheating.util.Http;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

	private final TaskService taskService;

	public TaskController(TaskService taskService) {
		this.taskService = taskService;
	}

	@GetMapping("/{id}")
	public ResponseEntity<TaskState> getTaskState(@PathVariable String id) {
		var state = taskService.getState(id);
		return Http.ok(state);
	}
}
