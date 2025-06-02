package com.greendelta.bioheating.model;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbl_users")
public class User extends BaseEntity {

	@Column(name = "username")
	private String name;

	@Column(name = "password")
	private String password;

	@Column(name = "full_name")
	private String fullName;

	@Column(name = "is_admin")
	private boolean isAdmin;

	public String name() {
		return name;
	}

	public User name(String name) {
		this.name = name;
		return this;
	}

	public String password() {
		return password;
	}

	public User password(String password) {
		this.password = password;
		return this;
	}

	public String fullName() {
		return fullName;
	}

	public User fullName(String fullName) {
		this.fullName = fullName;
		return this;
	}

	public boolean isAdmin() {
		return isAdmin;
	}

	public User isAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
		return this;
	}

	public static Res<String> hashPassword(String pw) {
		if (pw == null || pw.isBlank())
			return Res.error("invalid password provided");
		var hash = new BCryptPasswordEncoder().encode(pw);
		return Res.of(hash);
	}
}
