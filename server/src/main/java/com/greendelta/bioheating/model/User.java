package com.greendelta.bioheating.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbl_users")
public class User extends BaseEntity {

	private String email;
	private String name;
	private String password;
	private boolean isAdmin;

	public String email() {
		return email;
	}

	public User email(String email) {
		this.email = email;
		return this;
	}

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

	public boolean isAdmin() {
		return isAdmin;
	}

	public User isAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
		return this;
	}
}
