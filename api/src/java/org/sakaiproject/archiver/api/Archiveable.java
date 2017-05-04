package org.sakaiproject.archiver.api;

/**
 * Services which implement {@link Archiveable} declare that they are able to archive themselves.
 *
 * Any service implementing this must also register themselves with the {@link ArchiverRegistry}.
 *
 * To actually create the aggregated archive, you must use the methods in {@link ArchiverService}.
 *
 * @since 12.0
 * @author Steve Swinsburg (steve.swinsburg@gmail.com)
 */
public interface Archiveable {

	/**
	 * Create the tool archive
	 *
	 * @param archiveId the id of the archive that this tool should use to send the content to
	 * @param includeStudentContent if student content should be included in the archive
	 */
	void archive(String archiveId, boolean includeStudentContent);

}
