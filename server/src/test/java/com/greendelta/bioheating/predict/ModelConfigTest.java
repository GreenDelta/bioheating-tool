package com.greendelta.bioheating.predict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModelConfigTest {

	@TempDir
	File dir;

	@Test
	void writesAndReadsTheConfig() {
		var config = new ModelConfig(
			"test model", new Correction(1.2, -3), new Correction(0.9, 0.5));
		assertTrue(config.write(dir).isOk());
		assertEquals(config, ModelConfig.read(dir).orElseThrow());
	}

	@Test
	void readsAConfigWithoutDescription() throws Exception {
		write("""
			{
			  "corrections": {
			    "heatDemand": { "a": 2, "b": 1 },
			    "peakLoad": { "a": 1.0, "b": 0.0 }
			  }
			}""");
		var config = ModelConfig.read(dir).orElseThrow();
		assertNull(config.description());
		assertEquals(new Correction(2, 1), config.heatDemand());
		assertEquals(Correction.NONE, config.peakLoad());
	}

	@Test
	void failsForAMissingFile() {
		var res = ModelConfig.read(dir);
		assertTrue(res.isError());
		assertTrue(res.error().contains("not found"));
	}

	@Test
	void failsForMissingCorrections() throws Exception {
		write("""
			{ "corrections": { "heatDemand": { "a": 1, "b": 0 } } }""");
		var res = ModelConfig.read(dir);
		assertTrue(res.isError());
		assertTrue(res.error().contains("peakLoad"));
	}

	@Test
	void failsForInvalidValues() throws Exception {
		write("""
			{
			  "corrections": {
			    "heatDemand": { "a": "1", "b": 0 },
			    "peakLoad": { "a": 1, "b": 0 }
			  }
			}""");
		assertTrue(ModelConfig.read(dir).isError());
	}

	@Test
	void failsForInvalidJson() throws Exception {
		write("{ not json");
		assertTrue(ModelConfig.read(dir).isError());
	}

	private void write(String json) throws Exception {
		Files.writeString(new File(dir, ModelConfig.FILE_NAME).toPath(), json);
	}
}
