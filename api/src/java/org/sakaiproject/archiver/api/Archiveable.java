package org.sakaiproject.archiver.api;

/**
 * Services which implement {@link Archiveable} declare that they are able to archive themselves. 
 * Any service implementing this must also register themselves with the {@link ArchiverRegistry}.
 * To actually create the aggregated archive, you must use the methods in {@link ArchiverService}.
 *
 * @since 12.0
 * @author Steve Swinsburg (steve.swinsburg@gmail.com)
 */
public interface Archiveable {

	/**
	 * Create the tool archive
	 * @param siteId what site to create the tool's archive for
	 * @param includeStudentContent if student content should be included in the archive
	 */
	void archive(String siteId, boolean includeStudentContent);
	
}
