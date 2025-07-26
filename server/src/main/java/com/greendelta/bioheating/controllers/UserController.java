package com.greendelta.bioheating.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greendelta.bioheating.model.User;
import com.greendelta.bioheating.services.UserService;
import com.greendelta.bioheating.services.UserService.UserData;
import com.greendelta.bioheating.services.UserService.UserInfo;
import com.greendelta.bioheating.util.Http;

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
			: Http.badRequest("not authenticated");
	}

	@GetMapping("/{id}")
	public ResponseEntity<?> getUser(
		Authentication auth, @PathVariable int id
	) {
		var req = UserRequest.of(this, auth, id);
		return req.isError()
			? req.error()
			: Http.ok(UserInfo.of(req.user()));
	}

	@PostMapping
	public ResponseEntity<?> createUser(
		Authentication auth, @RequestBody UserData data
	) {
		if (Http.isNotAdmin(users, auth))
			return Http.badRequest("only admins can create or update users");
		var res = users.create(data);
		return res.hasError()
			? Http.badRequest("invalid user data: " + res.error())
			: Http.ok(res.value());
	}

	@PutMapping("/{id}")
	public ResponseEntity<?> updateUser(
		Authentication auth, @PathVariable long id, @RequestBody UserData data
	) {
		var req = UserRequest.of(this, auth, id);
		if (req.isError())
			return req.error();
		var res = users.update(req.user(), data);
		return res.hasError()
			? Http.badRequest("invalid user data: " + res.error())
			: Http.ok(res.value());
	}

	@GetMapping
	public ResponseEntity<?> getUsers(Authentication auth) {
		if (Http.isNotAdmin(users, auth))
			return Http.forbidden("only allowed for admins");
		var infos = users.getUserInfos();
		return Http.ok(infos);
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
			if (caller == null)
				return error(Http.forbidden("Not authenticated"));

			// fine for every user to get its own data;
			// otherwise only allowed for admins
			if (id == caller.id())
				return ok(caller);
			if (!caller.isAdmin())
				return error(Http.forbidden("Not allowed"));


			var user = self.users.get(id).orElse(null);
			return user != null
				? ok(user)
				: error(Http.notFound("That user does not exist"));
		}

		boolean isError() {
			return error != null;
		}
	}


}
