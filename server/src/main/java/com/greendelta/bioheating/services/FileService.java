package com.greendelta.bioheating.services;

import org.openlca.commons.Res;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.zip.ZipInputStream;

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

	/// Saves the uploaded files in the work directory. Note that multi-part files
	/// are deleted when a request finishes, so this needs to be done within the
	/// respective request thread.
	public Res<List<File>> saveUploads(MultipartFile[] uploads) {
		if (uploads == null) {
			return Res.error("No upload files provided");
		}

		var files = new ArrayList<File>();
		try {

			for (var u : uploads) {
				if (u == null || u.isEmpty()) continue;

				if ("zip".equals(extensionOf(u.getOriginalFilename()))) {
					extractZip(u, files);
					continue;
				}

				try (var stream = u.getInputStream()) {
					var ext = extensionOf(u.getOriginalFilename());
					var path = workDir.resolve(UUID.randomUUID() + "." + ext);
					Files.copy(stream, path);
					files.add(path.toFile());
				}
			}

			return Res.ok(files);
		} catch (Exception e) {
			for (var file : files) {
				drop(file.toPath());
			}
			return Res.error("Failed to save upload files", e);
		}
	}

	/// Calls the given function on the provided files and deletes them afterward.
	public <T> Res<T> useFiles(
		List<File> files, Function<List<File>, Res<T>> fn
	) {
		if (files == null || files.isEmpty() || fn == null) {
			return Res.error("No files or file handler provided");
		}
		try {
			return fn.apply(files);
		} catch (Exception e) {
			return Res.error("Failed to process files", e);
		} finally {
			for (var file : files) {
				drop(file.toPath());
			}
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
			LoggerFactory.getLogger(getClass()).error(
				"failed to delete file in work dir",
				e
			);
		}
	}

	private void extractZip(
		MultipartFile upload, List<File> files) throws IOException {
		try (var stream = upload.getInputStream();
				 var zip = new ZipInputStream(stream)) {
			for (var e = zip.getNextEntry(); e != null; e = zip.getNextEntry()) {
				try {
					if (e.isDirectory()) continue;
					var ext = extensionOf(e.getName());
					var path = workDir.resolve(UUID.randomUUID() + "." + ext);
					Files.copy(zip, path);
					files.add(path.toFile());
				} finally {
					zip.closeEntry();
				}
			}
		}
	}

	private String extensionOf(String fileName) {
		if (fileName == null) return "";
		var parts = fileName.toLowerCase().split("\\.");
		return parts.length > 1
			? parts[parts.length - 1].strip()
			: "";
	}

}
