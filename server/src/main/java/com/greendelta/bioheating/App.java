package com.greendelta.bioheating;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling: for scheduled methods like the cleanup of old task
// results in the `TaskService`
@SpringBootApplication
@EnableScheduling
public class App {

	static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}
}
