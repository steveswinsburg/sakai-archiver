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
	 * @param siteId the id of the site to get data for
	 * @param tooLid the well known id of the tool to get the content for. Useful if the same {@link Archiveable} is registered for multiple
	 *            tools
	 * @param includeStudentContent if student content (if applicable for the tool) should be included in the archive.
	 */
	void archive(String archiveId, String siteId, String toolId, boolean includeStudentContent);

}
