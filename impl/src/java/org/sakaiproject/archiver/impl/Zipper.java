package org.sakaiproject.archiver.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides methods to zip a directory
 */
public class Zipper {

	private static Logger log = LoggerFactory.getLogger(Zipper.class);

	/**
	 * Zip a directory. It is stored alongside the given directory.
	 *
	 * @param directory directory to be zipped
	 * @return the zip file path
	 * @throws IOException
	 */
	public static String zipDirectory(final File directory) throws IOException {

		// create path to zip
		final File zipFile = new File(directory.getParent(), directory.getName() + ".zip");

		if (!zipFile.exists()) {
			log.info("Creating zip file: " + zipFile.getCanonicalPath());
			zipFile.createNewFile();
		}

		FileOutputStream fOut = null;
		BufferedOutputStream bOut = null;
		ZipArchiveOutputStream zOut = null;

		try {
			fOut = new FileOutputStream(zipFile);
			bOut = new BufferedOutputStream(fOut);
			zOut = new ZipArchiveOutputStream(bOut);
			addFileToZip(zOut, directory); // add the directory which will then add all files recursively

		} finally {
			zOut.finish();
			zOut.close();
			bOut.close();
			fOut.close();
		}

		return zipFile.getCanonicalPath();
	}

	/**
	 * Creates a zip entry for the path specified with a name built from the base passed in and the file/directory name. If the path is a
	 * directory, a recursive call is made such that the full directory is added to the zip.
	 *
	 * @param zOut The zip file's output stream
	 * @param path The filesystem path of the file/directory being added
	 *
	 * @throws IOException If anything goes wrong
	 */
	private static void addFileToZip(final ZipArchiveOutputStream zOut, final File file) throws IOException {
		final ZipArchiveEntry zipEntry = new ZipArchiveEntry(file, file.getName());

		zOut.putArchiveEntry(zipEntry);

		if (file.isFile()) {
			FileInputStream fInputStream = null;
			try {
				fInputStream = new FileInputStream(file);
				IOUtils.copy(fInputStream, zOut);
				zOut.closeArchiveEntry();
			} finally {
				IOUtils.closeQuietly(fInputStream);
			}

		} else {
			zOut.closeArchiveEntry();
			final File[] children = file.listFiles();

			if (children != null) {
				for (final File child : children) {
					addFileToZip(zOut, child);
				}
			}
		}
	}
}
