package com.greendelta.bioheating.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

public class FileServiceTest {

	private Path workDir;
	private FileService service;

	@BeforeEach
	public void setup() throws IOException {
		workDir = Files.createTempDirectory("bioheating-file-service");
		service = new FileService(workDir.toString());
	}

	@AfterEach
	public void cleanup() throws IOException {
		try (var files = Files.list(workDir)) {
			for (var file : files.toList()) {
				Files.deleteIfExists(file);
			}
		}
		Files.deleteIfExists(workDir);
	}

	@Test
	public void testSaveUploads() throws IOException {
		var direct = new MockMultipartFile(
			"file",
			"direct.gml",
			"application/gml+xml",
			"<direct />".getBytes(StandardCharsets.UTF_8)
		);
		var zip = new MockMultipartFile(
			"file",
			"citygml.zip",
			"application/zip",
			zipOf(Map.of(
				"nested/building-a.xml", "<a />",
				"building-b.gml", "<b />"
			))
		);

		var result = service.saveUploads(new MockMultipartFile[]{direct, zip});

		assertFalse(result.isError());
		var files = result.value();
		assertEquals(3, files.size());
		var contents = files.stream()
			.map(file -> {
				try {
					return Files.readString(file.toPath());
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			})
			.collect(Collectors.toSet());
		assertEquals(Set.of("<direct />", "<a />", "<b />"), contents);
		assertTrue(files.stream().anyMatch(file -> file.getName().endsWith(".gml")));
		assertTrue(files.stream().anyMatch(file -> file.getName().endsWith(".xml")));

		for (var file : files) {
			Files.deleteIfExists(file.toPath());
		}
	}

	private byte[] zipOf(Map<String, String> entries) throws IOException {
		try (var bytes = new ByteArrayOutputStream();
			 var zip = new ZipOutputStream(bytes)) {
			for (var entry : entries.entrySet()) {
				zip.putNextEntry(new ZipEntry(entry.getKey()));
				zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
				zip.closeEntry();
			}
			zip.finish();
			return bytes.toByteArray();
		}
	}
}
