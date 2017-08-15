package org.sakaiproject.archiver.persistence;

import java.util.List;

import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.entity.ArchiveEntity;

/**
 * DAO for the Archiver. This is not part of the API and you should not use it. Use {@link ArchiverService} instead.
 */
public interface ArchiverPersistenceService {

	/**
	 * Create a new archive
	 *
	 * @param siteId
	 * @param userUuid
	 * @return the persistent entity
	 */
	ArchiveEntity create(String siteId, String userUuid);

	/**
	 * Update an existing archive
	 *
	 * @param entity the entity with updated fields to be persisted
	 * @return the updated entity
	 */
	ArchiveEntity update(ArchiveEntity entity);

	/**
	 * Get the latest archive for the given site. Return null if none exists
	 *
	 * @param siteId
	 * @return
	 */
	ArchiveEntity getLatest(String siteId);

	/**
	 * Get an archive by its id. Returns null if none exists by that id.
	 *
	 * @param archiveId the id to lookup the archive for
	 * @return
	 */
	ArchiveEntity getByArchiveId(String archiveId);

	/**
	 * Get a list of archives by the associated siteId ordered by date descending. The siteId can be left blank to get archives for all
	 * sites.
	 *
	 * @param siteId the id to lookup the archives for
	 * @param max the maximum number to return
	 * @return List of {@link ArchiveEntity} or empty list if none exist
	 */
	List<ArchiveEntity> getBySiteId(String siteId, int max);

}
