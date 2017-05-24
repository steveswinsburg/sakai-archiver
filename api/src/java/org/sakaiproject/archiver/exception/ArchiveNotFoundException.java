package org.sakaiproject.archiver.exception;

/**
 * Exception indicating that the archive could not be found either in the database or on the filesystem
 */
public class ArchiveNotFoundException extends Exception {
	private static final long serialVersionUID = 1L;

	public ArchiveNotFoundException() {
		super();
	}

	public ArchiveNotFoundException(final String message) {
		super(message);
	}

}
