package org.sakaiproject.archiver.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.sakaiproject.archiver.exception.ZipWriteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides methods to zip a directory
 */
public class Zipper {

	private Zipper() {
	}

	private static Logger log = LoggerFactory.getLogger(Zipper.class);

	/**
	 * Zip a directory. It is stored alongside the given directory.
	 *
	 * @param directory directory to be zipped
	 * @return the zip file path
	 * @throws IOException
	 * @throws ZipWriteException
	 */
	public static String zipDirectory(final File directory, final String name) throws IOException, ZipWriteException {

		// create path to zip
		final File zipFile = new File(directory.getParent(), name + ".zip");

		final Path p = Files.createFile(Paths.get(zipFile.getCanonicalPath()));
		try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p))) {
			final Path pp = Paths.get(directory.getCanonicalPath());
			Files.walk(pp)
					.filter(path -> !Files.isDirectory(path))
					.forEach(path -> {
						final ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
						try {
							zs.putNextEntry(zipEntry);
							zs.write(Files.readAllBytes(path));
							zs.closeEntry();
						} catch (final IOException e) {
							log.error("Error creating zip file", e);
							throw new ZipWriteException("Error creating zip file", e);
						}
					});
		}
		return zipFile.getCanonicalPath();
	}
}
