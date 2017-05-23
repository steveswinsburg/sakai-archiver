package org.sakaiproject.archiver.exception;

/**
 * Exception indicating that the file to archive was too large according to the configuration
 */
public class FileSizeExceededException extends Exception {
	private static final long serialVersionUID = 1L;

	public FileSizeExceededException() {
		super();
	}

}
