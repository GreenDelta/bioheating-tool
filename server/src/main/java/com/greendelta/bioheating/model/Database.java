package com.greendelta.bioheating.model;

import java.util.Objects;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class Database implements AutoCloseable {

	private final HikariDataSource pool;

	public static Config of(String name) {
		return new Config(name);
	}

	private Database(Config config) {
		var poolConf = new HikariConfig();
		poolConf.setJdbcUrl(config.url());
		poolConf.setUsername(config.user);
		poolConf.setPassword(config.password);
		pool = new HikariDataSource(poolConf);
	}

	@Override
	public void close() {
		pool.close();
	}

	public static class Config {
		private final String database;
		private String user = "postgres";
		private String password = "password";
		private String host = "localhost";
		private int port = 5432;

		private Config(String database) {
			this.database = Objects.requireNonNull(database);
		}

		public Config withUser(String user, String password) {
			this.user = Objects.requireNonNull(user);
			this.password = Objects.requireNonNull(password);
			return this;
		}

		public Config withHost(String host, int port) {
			this.host = Objects.requireNonNull(host);
			this.port = port;
			return this;
		}

		private String url() {
			return "jdbc:postgresql://" + host + ":" + port + "/" + database;
		}

		public Database connect() {
			return new Database(this);
		}
	}
}
