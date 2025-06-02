package com.greendelta.bioheating.config;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.User;

import jakarta.annotation.PreDestroy;

@Configuration
public class DataConfig {

	private Database db;

	@Bean
	public Database database(
		@Value("${db.name}") String name,
		@Value("${db.user:postgres}") String user,
		@Value("${db.password}") String password,
		@Value("${db.host:localhost}") String host,
		@Value("${db.port:5432}") int port
	) {
		db = Database.of(name)
			.withUser(user, password)
			.withHost(host, port)
			.connect();
		ensureAdmin(db);
		return db;
	}

	@PreDestroy
	public void close() {
		if (db != null) {
			db.close();
		}
	}

	private void ensureAdmin(Database db) {
		var log = LoggerFactory.getLogger(getClass());

		log.info("check that there is at least one admin in the database");
		for (var u : db.getAll(User.class)) {
			if (u.isAdmin()) {
				log.info("found admin {}", u.name());
				return;
			}
		}

		var hash = User.hashPassword("admin").orElseThrow();
		var admin = new User()
			.name("admin")
			.password(hash)
			.isAdmin(true);
		db.insert(admin);
		log.info("created default `admin`");
	}
}
