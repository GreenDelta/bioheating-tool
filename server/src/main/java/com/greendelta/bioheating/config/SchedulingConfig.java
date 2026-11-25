package com.greendelta.bioheating.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/// Enables scheduled methods like the cleanup of old task results. Note that
/// the background tasks have their own thread pool, see `TaskService`.
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
