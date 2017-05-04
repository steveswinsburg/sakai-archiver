package org.sakaiproject.archiver.exception;

/**
 * Exception indicating that no tools were specified to archive
 */
public class ToolsNotSpecifiedException extends Exception {
	private static final long serialVersionUID = 1L;

	public ToolsNotSpecifiedException(final String message) {
		super(message);
	}

}
