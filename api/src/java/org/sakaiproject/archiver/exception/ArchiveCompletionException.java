package org.sakaiproject.archiver.exception;

/**
 * Exception indicating that the archive could not be completed
 */
public class ArchiveCompletionException extends Exception {
	private static final long serialVersionUID = 1L;

	public ArchiveCompletionException(final String message, final Throwable e) {
		super(message, e);
	}

}
