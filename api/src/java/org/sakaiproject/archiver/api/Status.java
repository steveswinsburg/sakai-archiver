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
	 * Archive failed
	 */
	FAILED;

}
