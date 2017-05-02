package org.sakaiproject.archiver.api;

/**
 * Service for performing an archive. Provides methods that all implementers will need to use to get their content into the aggregated
 * archive.
 *
 * @since 12.0
 * @author Steve Swinsburg
 */
public interface ArchiverService {

	/**
	 * Checks if an archive is in progress for the given site.
	 *
	 * @return true/false
	 */
	boolean isArchiveInProgress(final String siteId);

	/**
	 * Start creating an archive for this site
	 *
	 * @param siteId siteId to archive
	 * @param userUuid userUuid who started the archive
	 * @param includeStudentData if student data is to be included
	 * @param toolIds the id of the tool in the site
	 */
	void startArchive(final String siteId, final String userUuid, final boolean includeStudentData, final String... toolIds);

}
