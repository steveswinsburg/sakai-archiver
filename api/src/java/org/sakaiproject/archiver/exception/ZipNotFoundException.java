package org.sakaiproject.archiver.exception;

/**
 * Exception indicating that the zip file could not be found
 */
public class ZipNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ZipNotFoundException(final String message) {
		super(message);
	}

}
