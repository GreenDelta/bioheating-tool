package com.greendelta.bioheating.controllers;

import com.greendelta.bioheating.model.User;
import com.greendelta.bioheating.services.ErrorCode;
import com.greendelta.bioheating.services.UserService;
import com.greendelta.bioheating.services.UserService.UserData;
import com.greendelta.bioheating.services.UserService.UserInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService users;

	public UserController(UserService users) {
		this.users = users;
	}

	@GetMapping("/current")
	public ResponseEntity<?> getCurrentUser(Authentication auth) {
		var user = users.getCurrentUser(auth).orElse(null);
		return user != null
			? Http.ok(UserInfo.of(user))
			: Http.unauthorized();
	}

	@PutMapping("/current/language")
	public ResponseEntity<?> setLanguage(
		Authentication auth,
		@RequestBody LanguageData data
	) {
		var user = users.getCurrentUser(auth).orElse(null);
		if (user == null) return Http.unauthorized();
		var res = users.setLanguage(user, data != null ? data.language() : null);
		return res.isError()
			? Http.badRequest(res)
			: Http.ok(res.value());
	}

	public record LanguageData(String language) {
	}

	@GetMapping("/{id}")
	public ResponseEntity<?> getUser(Authentication auth, @PathVariable int id) {
		var req = UserRequest.of(this, auth, id);
		return req.isError() ? req.error() : Http.ok(UserInfo.of(req.user()));
	}

	@PostMapping
	public ResponseEntity<?> createUser(
		Authentication auth,
		@RequestBody UserData data
	) {
		if (Http.isNotAdmin(users, auth)) return Http.forbidden(
			ErrorCode.ADMIN_REQUIRED
		);
		var res = users.create(data);
		return res.isError()
			? Http.badRequest(res)
			: Http.ok(res.value());
	}

	@PutMapping("/{id}")
	public ResponseEntity<?> updateUser(
		Authentication auth,
		@PathVariable long id,
		@RequestBody UserData data
	) {
		var req = UserRequest.of(this, auth, id);
		if (req.isError()) return req.error();
		var res = users.update(req.user(), data);
		return res.isError()
			? Http.badRequest(res)
			: Http.ok(res.value());
	}

	@GetMapping
	public ResponseEntity<?> getUsers(Authentication auth) {
		if (Http.isNotAdmin(users, auth)) return Http.forbidden(
			ErrorCode.ADMIN_REQUIRED
		);
		var infos = users.getUserInfos();
		return Http.ok(infos);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteUser(
		Authentication auth,
		@PathVariable long id
	) {
		var current = users.getCurrentUser(auth).orElse(null);
		if (current == null || !current.isAdmin()) return Http.forbidden(
			ErrorCode.ADMIN_REQUIRED
		);
		var req = UserRequest.of(this, auth, id);
		if (req.isError()) return req.error();
		var res = users.delete(req.user());
		return res.isError()
			? Http.badRequest(res)
			: Http.ok("User deleted successfully");
	}

	private record UserRequest(User user, ResponseEntity<?> error) {
		static UserRequest ok(User user) {
			return new UserRequest(user, null);
		}

		static UserRequest error(ResponseEntity<?> error) {
			return new UserRequest(null, error);
		}

		static UserRequest of(UserController self, Authentication auth, long id) {
			var caller = self.users.getCurrentUser(auth).orElse(null);
			if (caller == null) return error(Http.unauthorized());

			// fine for every user to get its own data;
			// otherwise only allowed for admins
			if (id == caller.id()) return ok(caller);
			if (!caller.isAdmin()) return error(Http.forbidden(ErrorCode.ACCESS_DENIED));

			var user = self.users.get(id).orElse(null);
			return user != null
				? ok(user)
				: error(Http.notFound(ErrorCode.USER_NOT_FOUND, "user not found: " + id));
		}

		boolean isError() {
			return error != null;
		}
	}
}
