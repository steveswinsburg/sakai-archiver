package org.sakaiproject.archiver.provider;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.spi.Archiveable;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link Archiveable} for the web content tools
 *
 * @author Steve Swinsburg(steve.swinsburg@gmail.com)
 * @since 12.0
 */
@Slf4j
public class WebContentArchiver implements Archiveable {

	private static final String TOOL_ID = "sakai.iframe";
	private static final String TOOL_NAME = "Web Content";

	public void init() {
		ArchiverRegistry.getInstance().register(TOOL_ID, this);
	}

	public void destroy() {
		ArchiverRegistry.getInstance().unregister(TOOL_ID);
	}

	@Setter
	private SiteService siteService;

	@Setter
	private ArchiverService archiverService;

	@Override
	public void archive(final String archiveId, final String siteId, final boolean includeStudentContent) {

		final Set<ToolConfiguration> tools = getTools(siteId, TOOL_ID);
		tools.forEach(t -> {
			final String url = t.getPlacementConfig().getProperty("source");
			final String filename = t.getTitle();
			final String fileContents = createUrlFileContents(url);
			this.archiverService.archiveContent(archiveId, siteId, TOOL_NAME, fileContents.getBytes(), filename + ".url");
		});

	}

	@Override
	public String getToolName(final String siteId, String toolId) {
		return TOOL_NAME;
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
