package com.greendelta.bioheating.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.greendelta.bioheating.model.ConstructionAge;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.ObjectMapper;

class JacksonConfigTest {

	private final ApplicationContextRunner context = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
		.withUserConfiguration(JacksonConfig.class);

	/// `ConstructionAge` overrides `toString()` but the UI expects the enum
	/// names; there must be a single mapper that is configured for this.
	@Test
	void usesEnumNames() {
		context.run(ctx -> {
			var json = ctx.getBean(ObjectMapper.class);
			assertEquals(
				"\"AGE_2010_2030\"",
				json.writeValueAsString(ConstructionAge.AGE_2010_2030));
			assertEquals(
				ConstructionAge.AGE_2010_2030,
				json.readValue("\"AGE_2010_2030\"", ConstructionAge.class));
		});
	}
}
