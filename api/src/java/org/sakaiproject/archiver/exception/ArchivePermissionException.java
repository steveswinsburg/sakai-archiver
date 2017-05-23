package org.sakaiproject.archiver.exception;

/**
 * Exception indicating that the user does not have the correct permissions for the archiver
 */
public class ArchivePermissionException extends Exception {
	private static final long serialVersionUID = 1L;

	public ArchivePermissionException(final String message) {
		super(message);
	}

}
