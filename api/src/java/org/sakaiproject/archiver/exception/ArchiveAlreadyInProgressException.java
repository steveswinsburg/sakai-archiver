package org.sakaiproject.archiver.exception;

/**
 * Exception indicating that an archive is already in progress for the selected site
 */
public class ArchiveAlreadyInProgressException extends Exception {
	private static final long serialVersionUID = 1L;

	public ArchiveAlreadyInProgressException(final String message) {
		super(message);
	}

}
