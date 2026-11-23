package com.greendelta.bioheating.services;

import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.model.User;
import com.greendelta.bioheating.services.ErrorCode.Coded;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import org.openlca.commons.Strings;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class UserService {

	/// The UI languages that are currently supported.
	public static final Set<String> LANGUAGES = Set.of("en", "de");

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
		if (Strings.isBlank(name)) return Optional.empty();
		for (var u : db.getAll(User.class)) {
			if (Strings.equalsIgnoreCase(name, u.name())) return Optional.of(u);
		}
		return Optional.empty();
	}

	public Coded<UserInfo> create(UserData data) {
		return data == null
			? ErrorCode.INVALID_USER_DATA.fail("No user data provided")
			: apply(new User(), data);
	}

	public Coded<UserInfo> update(User user, UserData data) {
		if (user == null || data == null) return ErrorCode.INVALID_USER_DATA.fail(
			"No user data provided"
		);

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
				return ErrorCode.LAST_ADMIN.fail("At least one admin must exist");
			}
		}

		return apply(user, data);
	}

	/// Sets the preferred UI language of the given user. A `null` or blank
	/// language resets it so that the UI uses the browser language again.
	public Coded<UserInfo> setLanguage(User user, String language) {
		if (user == null) return ErrorCode.USER_NOT_FOUND.fail("No user provided");
		var lang = Strings.isBlank(language) ? null : language.strip();
		if (lang != null && !LANGUAGES.contains(lang)) return ErrorCode
			.UNSUPPORTED_LANGUAGE.fail("Unsupported language: " + lang);
		user.language(lang);
		var updated = db.update(user);
		return Coded.ok(UserInfo.of(updated));
	}

	public Coded<Void> delete(User user) {
		if (user == null) return ErrorCode.USER_NOT_FOUND.fail("No user provided");
		if (user.isAdmin()) return ErrorCode.ADMIN_NOT_DELETABLE.fail(
			"Admins cannot be deleted"
		);
		// delete all projects of that user
		for (var p : db.getAll(Project.class)) {
			if (user.equals(p.user())) {
				db.delete(p);
			}
		}
		db.delete(user);
		return Coded.ok(null);
	}

	private Coded<UserInfo> apply(User user, UserData data) {
		var err = data.validate();
		if (err != null) return ErrorCode.INVALID_USER_DATA.fail(err);

		var other = getForName(data.name).orElse(null);
		if (other != null && !user.equals(other)) return ErrorCode
			.USER_NAME_EXISTS.fail("Another user with this name exists");

		var hash = User.hashPassword(data.password.strip());
		if (hash.isError()) return ErrorCode.INVALID_USER_DATA.fail(
			"Failed to process password"
		);

		user
			.name(data.name.strip())
			.fullName(data.fullName)
			.password(hash.value())
			.isAdmin(data.isAdmin);

		if (user.id() == 0) {
			db.insert(user);
			return Coded.ok(UserInfo.of(user));
		}

		var updated = db.update(user);
		return Coded.ok(UserInfo.of(updated));
	}

	public record UserInfo(
		long id,
		String name,
		String fullName,
		boolean isAdmin,
		String language
	) {
		public static UserInfo of(User user) {
			return new UserInfo(
				user.id(),
				user.name(),
				user.fullName(),
				user.isAdmin(),
				user.language()
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
			if (Strings.isBlank(name)) return "User name is empty";
			var n = name.strip();
			if (n.length() < 2) return "User name is shorter than 2 characters.";

			if (Strings.isBlank(password)) return "Password is empty";
			var p = password.strip();
			if (p.length() < 4) return "Password is too short";

			return null;
		}
	}
}
