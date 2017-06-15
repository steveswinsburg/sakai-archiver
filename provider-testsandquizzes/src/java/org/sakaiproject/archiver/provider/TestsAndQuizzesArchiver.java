package org.sakaiproject.archiver.provider;

import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.spi.Archiveable;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.entity.api.EntityManager;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link Archiveable} for the Tests and Quizzes (Samigo) tool
 *
 * @author Steve Swinsburg(steve.swinsburg@gmail.com)
 * @since 12.0
 */
@Slf4j
public class TestsAndQuizzesArchiver implements Archiveable {

	private static final String TOOL_ID = "sakai.samigo.tool";

	public void init() {
		ArchiverRegistry.getInstance().register(TOOL_ID, this);
	}

	public void destroy() {
		ArchiverRegistry.getInstance().unregister(TOOL_ID);
	}

	@Setter
	private ContentHostingService contentHostingService;

	@Setter
	private EntityManager entityManager;

	@Setter
	private ArchiverService archiverService;

	@Override
	public void archive(final String archiveId, final String siteId, final String toolId, final boolean includeStudentContent) {

	}

}
