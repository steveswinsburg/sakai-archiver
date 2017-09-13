package org.sakaiproject.archiver.exception;

/**
 * Exception indicating that the archive could not be cancelled
 */
public class ArchiveCancellationException extends Exception {
	private static final long serialVersionUID = 1L;

	public ArchiveCancellationException(final String message, final Throwable e) {
		super(message, e);
	}

	public ArchiveCancellationException(final String message) {
		super(message);
	}
}
