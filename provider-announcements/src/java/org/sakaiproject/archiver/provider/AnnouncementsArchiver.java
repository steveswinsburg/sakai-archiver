package org.sakaiproject.archiver.provider;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.sakaiproject.archiver.api.Archiveable;
import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link Archiveable} for the announcements tool
 */

@Slf4j
public class AnnouncementsArchiver implements Archiveable {

	private static final String HOME_TOOL = "sakai.announcements";

	public void init() {
		ArchiverRegistry.getInstance().register(HOME_TOOL, this);
	}

	public void destroy() {
		ArchiverRegistry.getInstance().unregister(HOME_TOOL);
	}

	@Setter
	private ArchiverService archiverService;

	@Override
	public void archive(final String archiveId, final String siteId, final String toolId, final boolean includeStudentContent) {

		log.info("Archiving {}", toolId);

	}

}
