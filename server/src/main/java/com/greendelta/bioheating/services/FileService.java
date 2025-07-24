package com.greendelta.bioheating.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.function.Function;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.greendelta.bioheating.util.Res;

@Service
public class FileService {

	private final Path workDir;

	public FileService(@Value("${work.dir}") String path) {
		workDir = Paths.get(path);
		try {
			if (!Files.exists(workDir)) {
				Files.createDirectories(workDir);
			}
		} catch (IOException e) {
			throw new RuntimeException("failed to create work directory: " + path, e);
		}
	}

	/// Uploads the file, calls the given function on that file, and deletes the
	/// file afterward.
	public <T> Res<T> useUpload(MultipartFile f, Function<File, Res<T>> fn) {
		if (f == null)
			return Res.error("no upload file provided");
		if (fn == null)
			return Res.error("no handler function for file provided");

		File file;
		try {
			var path = workDir.resolve(UUID.randomUUID().toString());
			try (var stream = f.getInputStream()) {
				Files.copy(stream, path);
			}
			file = path.toFile();
		} catch (Exception e) {
			return Res.error("failed upload file", e);
		}

		try {
			return fn.apply(file);
		} catch (Exception e) {
			return Res.error("failed to call file handler", e);
		} finally {
			drop(file.toPath());
		}
	}

	public <T> Res<T> withTempFile(String extension, Function<File, Res<T>> fn) {
		var file = workDir.resolve(UUID.randomUUID() + extension);
		try {
			return fn.apply(file.toFile());
		} catch (Exception e) {
			return Res.error("failed to call function on file", e);
		} finally {
			drop(file);
		}
	}

	private void drop(Path file) {
		try {
			Files.delete(file);
		} catch (Exception e) {
			LoggerFactory.getLogger(getClass())
				.error("failed to delete file in work dir", e);
		}
	}

}
