package com.greendelta.bioheating.services;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.greendelta.bioheating.TestModel;
import com.greendelta.bioheating.config.DataDir;
import java.io.File;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModelServiceTest {

	@TempDir
	File tempDir;

	@Test
	void failsWithAClearMessageWhenTheModelIsMissing() {
		var dataDir = new DataDir(tempDir.getAbsolutePath());
		var e = assertThrows(
			IllegalStateException.class,
			() -> new ModelService(dataDir)
		);
		assertTrue(e.getMessage().contains("Failed to load the prediction model"));
		assertTrue(e.getMessage().contains(dataDir.model().getAbsolutePath()));
	}

	@Test
	void loadsTheModelFromTheDataDirectory() {
		assumeTrue(TestModel.isAvailable(), TestModel.MISSING);
		var dataDir = new DataDir(TestModel.DIR.getAbsoluteFile().getParent());
		var service = new ModelService(dataDir);
		assertNotNull(service.predictor());
	}
}
