package com.greendelta.bioheating.util;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.greendelta.bioheating.model.User;
import com.greendelta.bioheating.services.UserService;

public final class Http {

	private Http() {
	}

	public static ResponseEntity<?> badRequest(String message) {
		return ResponseEntity.badRequest().body(message);
	}

	public static ResponseEntity<?> serverError(String message) {
		return ResponseEntity.internalServerError().body(message);
	}

	public static ResponseEntity<?> notFound(String message) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
	}

	public static ResponseEntity<?> forbidden(String message) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(message);
	}

	public static <T> ResponseEntity<T> ok(T entity) {
		return ResponseEntity.ok(entity);
	}

	public static boolean isNotAuthenticated(Authentication auth) {
		return auth == null || !auth.isAuthenticated();
	}

	public static Optional<User> getUser(UserService users, Authentication auth) {
		if (isNotAuthenticated(auth) || users == null)
			return Optional.empty();
		return users.getUser(auth.getName());
	}

	public static boolean isNotAdmin(UserService users, Authentication auth) {
		var user = getUser(users, auth).orElse(null);
		return user == null || !user.isAdmin();
	}
}

