package com.greendelta.bioheating.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Res;
import com.greendelta.bioheating.model.User;
import com.greendelta.bioheating.util.Strings;

@Service
public class UserService {

	private final Database db;

	public UserService(Database db) {
		this.db = db;
	}

	public List<UserInfo> getUserInfos() {
		var infos = new ArrayList<UserInfo>();
		for (var u : db.getAll(User.class)) {
			infos.add(UserInfo.of(u));
		}
		return infos;
	}

	public Optional<User> getUser(Authentication auth) {
		return auth != null && auth.isAuthenticated()
			? getUser(auth.getName())
			: Optional.empty();
	}

	public Optional<User> getUser(String name) {
		if (name == null || name.isBlank())
			return Optional.empty();
		for (var u : db.getAll(User.class)) {
			if (name.equalsIgnoreCase(u.name()))
				return Optional.of(u);
		}
		return Optional.empty();
	}

	public Optional<UserInfo> getUserInfo(String name) {
		var user = getUser(name).orElse(null);
		return user != null
			? Optional.of(UserInfo.of(user))
			: Optional.empty();
	}

	public Res<UserInfo> create(UserData data) {
		if (data == null)
			return Res.error("no user data provided");

		if (getUser(data.name).isPresent())
			return Res.error("a user '" + data.name + "' already exists");

		var nameRes = validateUserName(data.name);
		if (nameRes.hasError())
			return nameRes.castError();

		var pwRes = validatePassword(data.password);
		if (pwRes.hasError())
			return pwRes.castError();

		var hash = User.hashPassword(data.password);
		if (hash.hasError())
			return Res.error("failed to process password");

		var user = new User()
			.name(data.name)
			.fullName(data.fullName)
			.password(hash.value())
			.isAdmin(data.isAdmin);
		db.insert(user);
		return Res.of(UserInfo.of(user));
	}

	private Res<Void> validatePassword(String pw) {
		if (Strings.isNil(pw))
			return Res.error("password is empty");
		if (pw.length() < 4)
			return Res.error("password too short");
		for (var c : pw.toCharArray()) {
			if (Character.isWhitespace(c))
				return Res.error("password contains spaces");
		}
		return Res.VOID;
	}

	private Res<Void> validateUserName(String name) {
		if (Strings.isNil(name))
			return Res.error("user name is empty");
		if (name.length() < 2)
			return Res.error("user name too short");
		for (var c : name.toCharArray()) {
			if (Character.isAlphabetic(c) || Character.isDigit(c)
				|| c == '@' || c == '_' || c == '-' || c == '.' || c == '/')
				continue;
			return Res.error("invalid character in user name: '" + c + "'");
		}
		return Res.VOID;
	}

	public record UserInfo(
		String name,
		String fullName,
		boolean isAdmin
	) {

		public static UserInfo of(User user) {
			return new UserInfo(
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
		String url,
		boolean isAdmin
	) {
	}
}
