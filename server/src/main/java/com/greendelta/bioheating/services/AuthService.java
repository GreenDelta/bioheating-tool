package com.greendelta.bioheating.services;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.User;

@Service
public class AuthService implements UserDetailsService {

	private final Database db;

	public AuthService(Database db) {
		this.db = db;
	}

	@Override
	public UserDetails loadUserByUsername(
		String userName
	) throws UsernameNotFoundException {

		for (var u : db.getAll(User.class)) {
			if (u.name() == null)
				continue;
			if (!u.name().equalsIgnoreCase(userName))
				continue;

			var roles = u.isAdmin()
				? new String[]{"USER", "ADMIN"}
				: new String[]{"USER"};

			return org.springframework.security.core.userdetails.User.builder()
				.username(u.name())
				.password(u.password())
				.roles(roles)
				.build();
		}
		throw new UsernameNotFoundException("user not found: " + userName);
	}
}
