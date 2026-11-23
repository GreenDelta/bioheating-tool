package com.greendelta.bioheating.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/// The data directory of the application, configured via `app.data-dir`
/// (environment variable `APP_DATA_DIR`). It has the following layout:
///
/// ```
/// <data-dir>
///   model/    # the prediction model: *.ubj files and config.json
///   uploads/  # temporary files of uploads and exports
/// ```
@Component
public class DataDir {

	private final Path root;

	public DataDir(@Value("${app.data-dir}") String path) {
		this.root = Path.of(path).toAbsolutePath().normalize();
	}

	public Path root() {
		return root;
	}

	/// The directory of the prediction model. It is not created automatically
	/// as the model files need to be provided.
	public File model() {
		return root.resolve("model").toFile();
	}

	/// The directory for temporary files; created if it does not exist yet.
	public Path uploads() {
		var dir = root.resolve("uploads");
		try {
			Files.createDirectories(dir);
		} catch (IOException e) {
			throw new IllegalStateException(
				"Failed to create the uploads directory: " + dir, e
			);
		}
		return dir;
	}
}
