package org.sakaiproject.archiver.spi;

import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;

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
	 * @param includeStudentContent if student content (if applicable for the tool) should be included in the archive.
	 */
	void archive(String archiveId, String siteId, String toolId, boolean includeStudentContent);

	/**
	 * The toolId for this provider
	 *
	 * @return the toolid
	 */
	default String getToolId() {
		return "other";
	}

	/**
	 * The human readable name to be used for the directory within the archive
	 *
	 * @return the name
	 */
	default String getName() {
		return getToolId();
	}

}
