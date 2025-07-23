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
public class UploadService {

	private final Path workDir;

	public UploadService(@Value("${work.dir}") String path) {
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
	public <T> Res<T> useFile(MultipartFile f, Function<File, Res<T>> fn) {
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

		Res<T> res;
		try {
			res = fn.apply(file);
		} catch (Exception e) {
			res = Res.error("failed to call file handler", e);
		} finally {
			try {
				Files.delete(file.toPath());
			} catch (Exception e) {
				LoggerFactory.getLogger(getClass())
					.error("failed to delete file in work dir", e);
			}
		}

		return res;
	}

}
