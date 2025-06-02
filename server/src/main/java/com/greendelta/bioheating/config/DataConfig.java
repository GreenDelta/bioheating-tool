package com.greendelta.bioheating.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.greendelta.bioheating.model.Database;

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
		return db;
	}

	@PreDestroy
	public void close() {
		if (db != null) {
			db.close();
		}
	}
}
