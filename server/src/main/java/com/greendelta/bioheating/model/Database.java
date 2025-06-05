package com.greendelta.bioheating.model;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.eclipse.persistence.jpa.PersistenceProvider;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

public class Database implements AutoCloseable {

	private final HikariDataSource pool;
	private final EntityManagerFactory entityFactory;

	public static Config of(String name) {
		return new Config(name);
	}

	private Database(Config config) {

		// create the connection pool
		var poolConf = new HikariConfig();
		poolConf.setJdbcUrl(config.url());
		poolConf.setUsername(config.user);
		poolConf.setPassword(config.password);
		pool = new HikariDataSource(poolConf);

		// create the JPA persistence manager
		var jpaConfig = new HashMap<>();
		jpaConfig.put("jakarta.persistence.nonJtaDataSource", pool);
		jpaConfig.put("eclipselink.target-database", "PostgreSQL");
		entityFactory = new PersistenceProvider()
			.createEntityManagerFactory("bio-heating", jpaConfig);
	}

	public <T extends BaseEntity> T getForId(Class<T> type, long id) {
		try (var em = entityFactory.createEntityManager()) {
			return em.find(type, id);
		}
	}

	public <T extends BaseEntity> List<T> getAll(Class<T> type) {
		try (var em = entityFactory.createEntityManager()) {
			var q = em.createQuery(
				"SELECT e FROM " + type.getSimpleName() + " e", type);
			return q.getResultList();
		}
	}

	public <T extends BaseEntity> void insert(T entity) {
		withTransaction(em -> em.persist(entity));
	}

	public <T extends BaseEntity> T update(T entity) {
		var ref = new AtomicReference<>(entity);
		withTransaction(em -> ref.set(em.merge(entity)));
		return ref.get();
	}

	public <T extends BaseEntity> void delete(T entity) {
		withTransaction(em -> {
			var e = em.contains(entity)
				? entity
				: em.merge(entity);
			em.remove(e);
		});
	}

	private void withTransaction(Consumer<EntityManager> fn) {
		var em = entityFactory.createEntityManager();
		try {
			em.getTransaction().begin();
			fn.accept(em);
			em.getTransaction().commit();
		} catch (Exception e) {
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
			throw e;
		} finally {
			em.close();
		}
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
