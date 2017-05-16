package org.sakaiproject.archiver.provider;

import java.util.ArrayList;
import java.util.List;

import org.sakaiproject.archiver.api.Archiveable;
import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.provider.util.I18n;
import org.sakaiproject.service.gradebook.shared.GradebookService;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link Archiveable} for the gradebook tools
 *
 * @author Steve Swinsburg(steve.swinsburg@gmail.com)
 * @since 12.0
 */
@Slf4j
public class GradebookArchiver implements Archiveable {

	private static final String GRADEBOOK_CLASSIC_TOOL = "sakai.gradebook";
	private static final String GRADEBOOKNG_TOOL = "sakai.gradebookng";

	public void init() {
		ArchiverRegistry.getInstance().register(GRADEBOOK_CLASSIC_TOOL, this);
		ArchiverRegistry.getInstance().register(GRADEBOOKNG_TOOL, this);
	}

	public void destroy() {
		ArchiverRegistry.getInstance().unregister(GRADEBOOK_CLASSIC_TOOL);
		ArchiverRegistry.getInstance().unregister(GRADEBOOKNG_TOOL);

	}

	@Setter
	private GradebookService gradebookService;

	@Setter
	private ArchiverService archiverService;

	@Override
	public void archive(final String archiveId, final String siteId, final String toolId, final boolean includeStudentContent) {

		log.info("Archiving {}", toolId);

		// Create csv header
		final List<String> header = new ArrayList<String>();
		header.add(I18n.getString("export.header.studentId"));
		header.add(I18n.getString("export.header.studentName"));

		this.archiverService.archiveContent(archiveId, siteId, toolId, header.toString().getBytes(), "gradebook-export.csv");

	}

}
