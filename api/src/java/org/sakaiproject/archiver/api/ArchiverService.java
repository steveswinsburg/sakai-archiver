package org.sakaiproject.archiver.api;

/**
 * Service for performing an archive. Provides methods that all implementers will need to use to get their content into the aggregated archive.
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
	
	
	//void startArchive(final String siteId, final boolean includeStudentData, final String... toolIds);

}

