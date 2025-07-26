package com.greendelta.bioheating.config;

import java.io.IOException;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greendelta.bioheating.model.ClimateRegion;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Fuel;
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
		ensureBaseData(db);
		return db;
	}

	@PreDestroy
	public void close() {
		if (db != null) {
			db.close();
		}
	}

	private void ensureBaseData(Database db) {
		var log = LoggerFactory.getLogger(getClass());

		log.info("check that there is at least one admin in the database");
		for (var u : db.getAll(User.class)) {
			if (u.isAdmin()) {
				log.info("found admin {}", u.name());
				return;
			}
		}

		log.info("create default `admin`");
		createDefaultAdmin(db);

		int regionCount = importClimateRegions(db);
		if (regionCount >= 0) {
			log.info("import {} climate regions", regionCount);
		} else {
			log.error("failed to import climate regions");
		}

		int fuelCount = importFuels(db);
		if (fuelCount >= 0) {
			log.info("import {} fuels", fuelCount);
		} else {
			log.error("failed to import fuels");
		}
	}

	private void createDefaultAdmin(Database db) {
		var hash = User.hashPassword("admin").orElseThrow();
		var admin = new User()
			.name("admin")
			.fullName("Default Administrator")
			.password(hash)
			.isAdmin(true);
		db.insert(admin);
	}

	private int importClimateRegions(Database db) {
		var stream = getClass().getResourceAsStream("climate-regions.json");
		if (stream == null)
			return -1;

		try (stream) {
			var mapper = new ObjectMapper();
			var typeRef = new TypeReference<List<ClimateRegion>>() {
			};
			var regions = mapper.readValue(stream, typeRef);
			for (var data : regions) {
				db.insert(data);
			}
			return regions.size();
		} catch (IOException e) {
			LoggerFactory.getLogger(getClass())
				.error("failed to import climate regions", e);
			return -1;
		}
	}

	private int importFuels(Database db) {
		var stream = getClass().getResourceAsStream("fuels.json");
		if (stream == null)
			return -1;

		try (stream) {
			var mapper = new ObjectMapper();
			var typeRef = new TypeReference<List<Fuel>>() {
			};
			var fuels = mapper.readValue(stream, typeRef);
			for (var data : fuels) {
				db.insert(data);
			}
			return fuels.size();
		} catch (IOException e) {
			LoggerFactory.getLogger(getClass())
				.error("failed to import fuels", e);
			return -1;
		}
	}
}
