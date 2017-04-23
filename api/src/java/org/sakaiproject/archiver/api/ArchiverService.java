package org.sakaiproject.archiver.api;

public interface ArchiverService {

	/**
	 * Checks if an archive is in progress for the given site.
	 *
	 * @return true/false
	 */
	boolean isArchiveInProgress(final String siteId);
	
	
	//void startArchive(final String siteId, final boolean includeStudentData, final String... toolIds);

}

