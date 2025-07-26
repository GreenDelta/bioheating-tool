package com.greendelta.bioheating.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.User;
import com.greendelta.bioheating.util.Res;
import com.greendelta.bioheating.util.Strings;

@Service
public class UserService {

	private final Database db;

	public UserService(Database db) {
		this.db = db;
	}

	public Optional<User> get(long id) {
		return Optional.ofNullable(db.getForId(User.class, id));
	}

	public List<UserInfo> getUserInfos() {
		var infos = new ArrayList<UserInfo>();
		for (var u : db.getAll(User.class)) {
			infos.add(UserInfo.of(u));
		}
		return infos;
	}

	public Optional<User> getCurrentUser(Authentication auth) {
		return auth != null && auth.isAuthenticated()
			? getForName(auth.getName())
			: Optional.empty();
	}

	private Optional<User> getForName(String name) {
		if (Strings.isNil(name))
			return Optional.empty();
		for (var u : db.getAll(User.class)) {
			if (Strings.eq(name, u.name()))
				return Optional.of(u);
		}
		return Optional.empty();
	}

	public Res<UserInfo> create(UserData data) {
		return data == null
			? Res.error("No user data provided")
			: apply(new User(), data);
	}

	public Res<UserInfo> update(User user, UserData data) {
		if (user == null || data == null)
			return Res.error("No user data provided");

		// check that we always have an admin
		if (user.isAdmin() && !data.isAdmin()) {
			boolean otherAdmin = false;
			for (var u : db.getAll(User.class)) {
				if (!u.equals(user) && u.isAdmin()) {
					otherAdmin = true;
					break;
				}
			}
			if (!otherAdmin) {
				return Res.error("At least one admin must exist");
			}
		}

		return apply(user, data);
	}

	private Res<UserInfo> apply(User user, UserData data) {
		var err = data.validate();
		if (err != null)
			return Res.error("Invalid user data: " + err);

		var other = getForName(data.name).orElse(null);
		if (other != null && !user.equals(other))
			return Res.error("Another user with this name exists");

		var hash = User.hashPassword(data.password.strip());
		if (hash.hasError())
			return Res.error("Failed to process password");

		user.name(data.name.strip())
			.fullName(data.fullName)
			.password(hash.value())
			.isAdmin(data.isAdmin);

		if (user.id() == 0) {
			db.insert(user);
		} else {
			user = db.update(user);
		}

		return Res.of(UserInfo.of(user));
	}

	public record UserInfo(
		long id,
		String name,
		String fullName,
		boolean isAdmin
	) {

		public static UserInfo of(User user) {
			return new UserInfo(
				user.id(),
				user.name(),
				user.fullName(),
				user.isAdmin()
			);
		}
	}

	public record UserData(
		String name,
		String password,
		String fullName,
		boolean isAdmin
	) {

		String validate() {

			if (Strings.isNil(name))
				return "User name is empty";
			var n = name.strip();
			if (n.length() < 2)
				return "User name is shorter than 2 characters.";

			if (Strings.isNil(password))
				return "Password is empty";
			var p = password.strip();
			if (p.length() < 4)
				return "Password is too short";

			return null;
		}
	}
}
