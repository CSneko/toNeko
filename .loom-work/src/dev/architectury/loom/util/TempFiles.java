package dev.architectury.loom.util;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.loom.util.DeletingFileVisitor;

/**
 * A helper for temporary files and directories
 * that cleans up when closed by deleting all created
 * temp files.
 */
public final class TempFiles implements Closeable {
	private final List<Path> directories = new ArrayList<>();
	private final List<Path> files = new ArrayList<>();

	/**
	 * Creates a temporary directory.
	 *
	 * @param prefix a prefix for the directory's name
	 * @return the created directory
	 */
	public Path directory(@Nullable String prefix) throws IOException {
		final Path directory = Files.createTempDirectory(prefix);
		directories.add(directory);
		return directory;
	}

	/**
	 * Creates a temporary file.
	 *
	 * @param prefix a prefix for the file's name
	 * @param suffix a suffix for the file's name
	 * @return the created file
	 */
	public Path file(@Nullable String prefix, @Nullable String suffix) throws IOException {
		final Path file = Files.createTempFile(prefix, suffix);
		files.add(file);
		return file;
	}

	@Override
	public void close() throws IOException {
		final List<IOException> exceptions = new ArrayList<>();

		for (Path file : files) {
			try {
				Files.deleteIfExists(file);
			} catch (IOException e) {
				exceptions.add(e);
			}
		}

		files.clear();

		for (Path directory : directories) {
			if (Files.exists(directory)) {
				try {
					Files.walkFileTree(directory, new DeletingFileVisitor());
				} catch (IOException e) {
					exceptions.add(e);
				}
			}
		}

		directories.clear();

		if (!exceptions.isEmpty()) {
			final IOException root = new IOException("Could not delete temporary files", exceptions.get(0));

			for (int i = 1; i < exceptions.size(); i++) {
				root.addSuppressed(exceptions.get(i));
			}

			throw root;
		}
	}
}
