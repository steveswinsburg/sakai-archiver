package org.sakaiproject.archiver.api;

import java.util.List;

import org.sakaiproject.archiver.dto.Archive;
import org.sakaiproject.archiver.exception.ArchiveAlreadyInProgressException;
import org.sakaiproject.archiver.exception.ArchiveCompletionException;
import org.sakaiproject.archiver.exception.ArchiveInitialisationException;
import org.sakaiproject.archiver.exception.ArchiveNotFoundException;
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
	 * @throws {@link ArchiveCompletionException} if the archive could not be completed properly
	 */
	void startArchive(final String siteId, final String userUuid, final boolean includeStudentData, final String... toolIds)
			throws ToolsNotSpecifiedException, ArchiveAlreadyInProgressException, ArchiveInitialisationException,
			ArchiveCompletionException;

	/**
	 * Tools can call this to add content of a file into the archive
	 *
	 * @param archiveId the id of the archive that the content is for
	 * @param siteId that this archive is for
	 * @param content the content to be archived
	 * @param filename the name of the file that the content will be archived into. This should include the relevant extension.
	 */
	void archiveContent(final String archiveId, final String siteId, byte[] content, String filename);

	/**
	 * Tools can call this to add content of a file into the archive, with an optional set of subdirectory
	 *
	 * @param archiveId the id of the archive that the content is for
	 * @param siteId that this archive is for
	 * @param content the content to be archived
	 * @param filename the name of the file that the content will be archived into. This should include the relevant extension.
	 * @param subdirectories the subdirectories within the archive where the file will be written. Do not include any path separator, these
	 *            will be added automatically.
	 */
	void archiveContent(final String archiveId, final String siteId, byte[] content, String filename, final String... subdirectories);

	/**
	 * Get an archive for the given archiveId
	 *
	 * @param archiveId the id to get the archive for
	 * @return the {@link Archive} dto
	 * @throws {@ArchiveNotFoundException} if no archive can be found by that archiveId
	 */
	Archive getArchive(final String archiveId) throws ArchiveNotFoundException;

	/**
	 * Get the latest archive for the given siteId
	 *
	 * @param siteId the siteId to get the archive for
	 * @return the {@link Archive} dto or null if none;
	 */
	Archive getLatest(final String siteId);

	/**
	 * Get a list of archives for the given site
	 *
	 * @param siteId the siteId to get the archives for
	 * @return List of {@link Archive}
	 */
	List<Archive> getArchives(final String siteId);

}
