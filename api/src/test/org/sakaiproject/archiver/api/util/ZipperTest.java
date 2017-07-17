package org.sakaiproject.archiver.api.util;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.sakaiproject.archiver.exception.ZipWriteException;
import org.sakaiproject.archiver.util.Zipper;

/**
 * Test for the {@link Zipper} utility
 */
public class ZipperTest {

	@Test
	public void should_createZip() {

		final File currDir = new File(getClass().getResource("/").getFile());
		final File destDir = new File(currDir.getAbsolutePath() + File.separatorChar + "dest");

		try {
			FileUtils.copyDirectory(currDir, destDir);
		} catch (final IOException e) {
			Assert.fail("Couldn't copy resources for test");
		}

		String zipfilePath = null;
		try {
			zipfilePath = Zipper.zipDirectory(destDir, "output");
		} catch (final IOException | ZipWriteException e) {
			Assert.fail("Couldn't zip directory");
		}

		// ensure zip is created
		final File zip = new File(zipfilePath);
		Assert.assertTrue("Zip not created: " + zip.getAbsolutePath(), zip.isFile());

	}

}
