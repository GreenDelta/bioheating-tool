package com.greendelta.bioheating.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.EnumFeature;

/// Customizes the JSON mapper of Spring Boot, which is used for the HTTP
/// messages. We do not define our own mapper bean, as Spring Boot would then
/// still create its own one for the HTTP messages.
@Configuration
public class JacksonConfig {

	@Bean
	public JsonMapperBuilderCustomizer jsonCustomizer() {
		// Jackson 3 uses `toString()` for enums by default; the UI expects the
		// enum names (e.g. `AGE_2010_2030` and not the label `2010-2030`)
		return builder -> builder
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
			.disable(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
			.disable(EnumFeature.READ_ENUMS_USING_TO_STRING);
	}
}
