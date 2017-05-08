package org.sakaiproject.archiver.exception;

/**
 * Exception indicating that the archive could not be initialised
 */
public class ArchiveInitialisationException extends Exception {
	private static final long serialVersionUID = 1L;

	public ArchiveInitialisationException(final String message, final Throwable e) {
		super(message, e);
	}

}
