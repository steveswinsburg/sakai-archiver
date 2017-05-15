package org.sakaiproject.archiver.exception;

/**
 * Exception indicating that there was an error creating the zip
 */
public class ZipWriteException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ZipWriteException(final String message, final Throwable e) {
		super(message, e);
	}

}
