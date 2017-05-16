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
 * Implementation of {@link Archiveable} for various content tools
 *
 * @author Steve Siwnsburg(steve.swinsburg@gmail.com)
 * @since 12.0
 */
@Slf4j
public class ContentArchiver implements Archiveable {

	private static final String HOME_TOOL = "sakai.iframe";

	public void init() {
		ArchiverRegistry.getInstance().register(HOME_TOOL, this);
	}

	public void destroy() {
		ArchiverRegistry.getInstance().unregister(HOME_TOOL);
	}

	@Setter
	private SiteService siteService;

	@Setter
	private ArchiverService archiverService;

	@Override
	public void archive(final String archiveId, final String siteId, final String toolId, final boolean includeStudentContent) {

		log.info("Archiving {}", toolId);

		switch (toolId) {
			case HOME_TOOL: {
				archiveHomeTool(archiveId, siteId);
				break;
			}
			default: {
				log.error("No handler for {}", toolId);
			}
		}

	}

	/**
	 * Archive the home tool
	 *
	 * @param archiveId
	 * @param siteId
	 */
	public void archiveHomeTool(final String archiveId, final String siteId) {

		final Set<ToolConfiguration> tools = getTools(siteId, HOME_TOOL);
		tools.forEach(t -> {
			final String url = t.getPlacementConfig().getProperty("source");
			final String filename = t.getTitle();
			final String fileContents = createUrlFileContents(url);
			this.archiverService.archiveContent(archiveId, siteId, HOME_TOOL, fileContents.getBytes(), filename + ".url");
		});
	}

	/**
	 * Get all {@link ToolConfiguration}s in the site that match the supplied toolIds
	 *
	 * @param siteId the siteId to get tools for
	 * @param toolIds the toolIds to match
	 * @return
	 */
	private Set<ToolConfiguration> getTools(final String siteId, final String... toolIds) {

		Site site;
		try {
			site = this.siteService.getSite(siteId);
		} catch (final IdUnusedException e) {
			log.error("Invalid site. Cannot lookup tools");
			return Collections.emptySet();
		}

		return new HashSet<>(site.getTools(toolIds));
	}

	/**
	 * Create the content of the URL file
	 *
	 * @param url the URL to add to the file
	 * @return
	 */
	private String createUrlFileContents(final String url) {
		final StringBuilder sb = new StringBuilder();
		sb.append("[InternetShortcut]");
		sb.append(System.lineSeparator());
		sb.append("URL=" + url);
		return sb.toString();

	}
}
