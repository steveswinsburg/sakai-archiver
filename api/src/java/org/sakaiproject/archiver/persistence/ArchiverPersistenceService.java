package org.sakaiproject.archiver.persistence;

import org.sakaiproject.archiver.dto.Archive;

public interface ArchiverPersistenceService {

	/**
	 * Create a new archive
	 *
	 * @param siteId
	 * @param userUuid
	 * @return
	 */
	public Archive create(String siteId, String userUuid);

	/**
	 * Get a current archive for the given site. Return null if none exists or is not currently active.
	 *
	 * @param siteId
	 * @param userUuid
	 * @return
	 */
	public Archive getCurrent(String siteId);

}
