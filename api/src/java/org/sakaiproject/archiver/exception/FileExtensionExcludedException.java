package org.sakaiproject.archiver.exception;

/**
 * Exception indicating that the file to archive had an excluded extension according to the configuration
 */
public class FileExtensionExcludedException extends Exception {
	private static final long serialVersionUID = 1L;

	public FileExtensionExcludedException() {
		super();
	}

}
