package org.sakaiproject.archiver.provider;

import org.sakaiproject.archiver.api.Archiveable;
import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.content.api.ContentHostingService;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link Archiveable} for various content tools
 */
@Slf4j
public class AssignmentArchiver implements Archiveable {

	private static final String ASSIGNMENT_TOOL = "sakai.assignment";

	public void init() {
		ArchiverRegistry.getInstance().register(ASSIGNMENT_TOOL, this);
	}

	public void destroy() {
		ArchiverRegistry.getInstance().unregister(ASSIGNMENT_TOOL);
	}

	@Setter
	private ContentHostingService contentHostingService;

	@Setter
	private ArchiverService archiverService;

	@SuppressWarnings("unchecked")
	@Override
	public void archive(final String archiveId, final String siteId, final String toolId, final boolean includeStudentContent) {

	}
	
	/**
	 * Simplified helper class to represent an individual assignment item in a site
	 */
	public static class ArchiveItem {
		

	}
}
