package org.sakaiproject.archiver.persistence;

import org.sakaiproject.archiver.entity.ArchiveEntity;

public interface ArchiverPersistenceService {

	/**
	 * Create a new archive
	 *
	 * @param siteId
	 * @param userUuid
	 * @return the persistent entity
	 */
	public ArchiveEntity create(String siteId, String userUuid);

	/**
	 * Update an existing archive
	 *
	 * @param entity the entity with updated fields to be persisted
	 * @return the updated entity
	 */
	public ArchiveEntity update(ArchiveEntity entity);

	/**
	 * Get the current archive for the given site. Return null if none exists or is not currently active.
	 *
	 * @param siteId
	 * @param userUuid
	 * @return
	 */
	public ArchiveEntity getCurrent(String siteId);

}
