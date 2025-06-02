package com.greendelta.bioheating.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greendelta.bioheating.services.UserService;
import com.greendelta.bioheating.services.UserService.UserInfo;
import com.greendelta.bioheating.util.Http;
import com.greendelta.bioheating.util.Strings;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService users;

	public UserController(UserService users) {
		this.users = users;
	}

	@GetMapping("/current")
	public ResponseEntity<?> getCurrent(Authentication auth) {
		if (Http.isNotAuthenticated(auth))
			return Http.badRequest("not authenticated");
		var info = users.getUserInfo(auth.getName()).orElse(null);
		return info == null
			? Http.badRequest("unknown user " + auth.getName())
			: Http.ok(info);
	}

	@GetMapping("/{name}")
	public ResponseEntity<?> getUser(
		Authentication auth, @PathVariable String name
	) {
		var caller = Http.getUser(users, auth).orElse(null);
		if (caller == null)
			return Http.forbidden("not authenticated");
		if (!caller.isAdmin() && !Strings.eq(name, caller.name()))
			return Http.forbidden("not allowed to see user details");

		var user = users.getUser(name).orElse(null);
		return user == null
			? Http.notFound("could not find user: " + name)
			: Http.ok(UserInfo.of(user));
	}

	@PostMapping
	public ResponseEntity<?> postUser(
		Authentication auth, @RequestBody UserService.UserData data
	) {
		if (Http.isNotAdmin(users, auth))
			return Http.badRequest("only admins can create or update users");
		var res = users.create(data);
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
}

