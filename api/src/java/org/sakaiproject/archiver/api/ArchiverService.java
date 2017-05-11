package org.sakaiproject.archiver.api;

import org.sakaiproject.archiver.exception.ArchiveAlreadyInProgressException;
import org.sakaiproject.archiver.exception.ArchiveInitialisationException;
import org.sakaiproject.archiver.exception.ToolsNotSpecifiedException;

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
	 * Start creating an archive for this site.
	 *
	 * @param siteId siteId to archive
	 * @param userUuid userUuid who started the archive
	 * @param includeStudentData if student data is to be included
	 * @param toolIds the id of the tool in the site
	 *
	 * @throws {@link ToolsNotSpecifiedException} if no tools are specified
	 * @throws {@link ArchiveAlreadyInProgressException} if an archive is already in progress for the given site
	 * @throws {@link ArchiveInitialisationException} if the archive could not be initialised
	 */
	void startArchive(final String siteId, final String userUuid, final boolean includeStudentData, final String... toolIds)
			throws ToolsNotSpecifiedException, ArchiveAlreadyInProgressException, ArchiveInitialisationException;

	/**
	 * Tools can call this to add content of a file into the archive
	 *
	 * @param archiveId the id of the archive that the content is for
	 * @param toolId the tool that the archive is for
	 * @param content the content to be archived
	 * @param filename the name of the file that the content will be archived into. This should include the relevant extension.
	 */
	void archiveContent(final String archiveId, final String toolId, byte[] content, String filename);

	/**
	 * Tools can call this to add content of a file into the archive, with an optional subdirectory
	 *
	 * @param archiveId the id of the archive that the content is for
	 * @param toolId the tool that the archive is for
	 * @param content the content to be archived
	 * @param subdirectory the subdirectory where the file will be written
	 * @param filename the name of the file that the content will be archived into. This should include the relevant extension.
	 */
	void archiveContent(final String archiveId, final String toolId, byte[] content, String subdirectory, String filename);

}
