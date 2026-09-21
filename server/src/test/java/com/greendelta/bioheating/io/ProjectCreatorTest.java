package com.greendelta.bioheating.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.greendelta.bioheating.model.Project;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class ProjectCreatorTest {

	private final List<File> tempFiles = new ArrayList<>();

	@AfterEach
	public void cleanup() throws IOException {
		for (var file : tempFiles) {
			Files.deleteIfExists(file.toPath());
		}
	}

	@Test
	public void testUnsupportedFilesAreSkipped() {
		var creator = new ProjectCreator(
			null,
			new Project().name("test project"),
			List.of(tempFile("notes.txt"), tempFile("data.csv")));

		var res = creator.importFiles();

		// unsupported files must not be handed to an importer; when nothing
		// importable is left, the import fails with a clear error
		assertTrue(res.isError());
		assertEquals("No import files provided", res.error());
	}

	@Test
	public void testNoFilesProvided() {
		var creator = new ProjectCreator(
			null, new Project().name("test project"), List.of());

		var res = creator.importFiles();

		assertTrue(res.isError());
		assertEquals("No import files provided", res.error());
	}

	private File tempFile(String name) {
		try {
			var path = Files.createTempFile("import-", "-" + name);
			tempFiles.add(path.toFile());
			return path.toFile();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
