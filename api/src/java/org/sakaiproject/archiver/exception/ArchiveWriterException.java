package org.sakaiproject.archiver.exception;

/**
 * Exception indicating that there was an error adding the supplied content to the archive
 */
public class ArchiveWriterException extends Exception {
	private static final long serialVersionUID = 1L;

	public ArchiveWriterException(final String message, final Throwable e) {
		super(message, e);
	}

}
