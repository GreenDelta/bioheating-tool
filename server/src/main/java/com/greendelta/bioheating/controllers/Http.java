package com.greendelta.bioheating.controllers;

import com.greendelta.bioheating.services.ErrorCode;
import com.greendelta.bioheating.services.ErrorCode.ApiError;
import com.greendelta.bioheating.services.ErrorCode.Coded;
import com.greendelta.bioheating.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

/// Helper methods for creating HTTP responses. Error responses always carry
/// an `ApiError` with an error code that is translated in the client.
final class Http {

	private Http() {}

	static ResponseEntity<?> error(
		HttpStatus status,
		ErrorCode code,
		String details
	) {
		return ResponseEntity.status(status).body(new ApiError(code, details));
	}

	static ResponseEntity<?> badRequest(ErrorCode code, String details) {
		return error(HttpStatus.BAD_REQUEST, code, details);
	}

	static ResponseEntity<?> badRequest(ErrorCode code) {
		return badRequest(code, null);
	}

	static ResponseEntity<?> badRequest(Coded<?> res) {
		return ResponseEntity.badRequest().body(res.apiError());
	}

	static ResponseEntity<?> unauthorized() {
		return error(
			HttpStatus.UNAUTHORIZED,
			ErrorCode.NOT_AUTHENTICATED,
			"Not authenticated"
		);
	}

	static ResponseEntity<?> serverError(ErrorCode code, String details) {
		return error(HttpStatus.INTERNAL_SERVER_ERROR, code, details);
	}

	static ResponseEntity<?> notFound(ErrorCode code, String details) {
		return error(HttpStatus.NOT_FOUND, code, details);
	}

	static ResponseEntity<?> forbidden(ErrorCode code) {
		return error(HttpStatus.FORBIDDEN, code, null);
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
