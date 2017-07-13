package org.sakaiproject.archiver.exception;

/**
 * Exception indicating that there was an error processing the archive
 */
public class ArchiveProcessingException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ArchiveProcessingException(final String message, final Throwable e) {
		super(message, e);
	}

}
