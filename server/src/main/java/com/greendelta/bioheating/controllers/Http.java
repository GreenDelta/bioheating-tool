package com.greendelta.bioheating.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.greendelta.bioheating.services.UserService;

final class Http {

	private Http() {}

	static ResponseEntity<?> badRequest(String message) {
		return ResponseEntity.badRequest().body(message);
	}

	static ResponseEntity<?> serverError(String message) {
		return ResponseEntity.internalServerError().body(message);
	}

	static ResponseEntity<?> notFound(String message) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
	}

	static ResponseEntity<?> forbidden(String message) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(message);
	}

	static <T> ResponseEntity<T> ok(T entity) {
		return ResponseEntity.ok(entity);
	}

	static boolean isNotAdmin(UserService users, Authentication auth) {
		if (users == null || auth == null) return true;
		var user = users.getCurrentUser(auth).orElse(null);
		return user == null || !user.isAdmin();
	}
}
