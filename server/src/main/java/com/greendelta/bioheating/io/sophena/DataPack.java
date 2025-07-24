package com.greendelta.bioheating.io.sophena;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class DataPack implements Closeable {

	private final FileSystem zip;
	private final ObjectMapper mapper;

	private DataPack(FileSystem zip) {
		this.zip = zip;
		this.mapper = new ObjectMapper();
	}

	static DataPack create(File file) throws IOException {
		Files.deleteIfExists(file.toPath());

		// copy the template with reference data
		var res = DataPack.class.getResourceAsStream("template.sophena");
		if (res != null) {
			try (res; var buff = new BufferedInputStream(res, 1024)) {
				Files.copy(buff, file.toPath());
			}
		}

		// open the zip
		String uriStr = file.toURI().toASCIIString();
		URI uri = URI.create("jar:" + uriStr);
		Map<String, String> opt = file.exists()
			? Map.of()
			: Map.of("create", "true");
		var zip = FileSystems.newFileSystem(uri, opt);
		return new DataPack(zip);
	}

	public void put(String path, JsonNode obj) throws IOException {
		if (path == null || obj == null)
			return;
		var p = zip.getPath(path);
		var dir = p.getParent();
		if (dir != null && !Files.exists(dir)) {
			Files.createDirectories(dir);
		}
		var json = mapper.writerWithDefaultPrettyPrinter()
			.writeValueAsString(obj);
		Files.writeString(p, json,
			StandardOpenOption.CREATE,
			StandardOpenOption.TRUNCATE_EXISTING);
	}

	@Override
	public void close() throws IOException {
		// write the package meta-data if not present
		var meta = zip.getPath("meta.json");
		if (!Files.exists(meta)) {
			var obj = mapper.createObjectNode();
			obj.put("version", 2);
			obj.put("generator", "BioHeating-Tool");
			obj.put("timestamp", Instant.now().toString());
			put("meta.json", obj);
		}
		zip.close();
	}
}
