package org.sakaiproject.archiver.api;

/**
 * List of statuses that an archive could be at
 */
public enum Status {

	/**
	 * Archive has been started
	 */
	STARTED,

	/**
	 * Archive is complete
	 */
	COMPLETE,

	/**
	 * Archive is finished but is incomplete
	 */
	INCOMPLETE,

	/**
	 * Archive was cancelled
	 */
	CANCELLED,

	/**
	 * Archive failed
	 */
	FAILED;

}
