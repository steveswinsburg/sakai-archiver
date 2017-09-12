package org.sakaiproject.archiver.provider;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.spi.Archiveable;
import org.sakaiproject.archiver.util.Htmlifier;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link Archiveable} for the news tool (the Simple RSS Portlet variety)
 *
 * @author Steve Swinsburg(steve.swinsburg@gmail.com)
 * @since 12.0
 */
@Slf4j
public class NewsArchiver implements Archiveable {

	private static final String TOOL_ID = "sakai.simple.rss";

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

		final String toolName = getToolName(siteId);

		final Set<ToolConfiguration> tools = getTools(siteId, TOOL_ID);
		tools.forEach(t -> {
			final String filename = t.getTitle();

			final String htmlBody = getAsHtml(getNewsData(t));
			final String html = Htmlifier.toHtml(htmlBody, this.archiverService.getSiteHeader(siteId, TOOL_ID));
			log.debug("news html: " + html);
			this.archiverService.archiveContent(archiveId, siteId, toolName, html.getBytes(), filename + ".html");

		});

	}

	@Override
	public String getToolName(final String siteId) {
		return this.archiverService.getToolName(siteId, TOOL_ID);
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
	 * Create the data for the file
	 *
	 * @param tool the {@link ToolConfiguration} to extract the data from
	 * @return
	 */
	private NewsData getNewsData(final ToolConfiguration tool) {
		final String url = decode(tool.getPlacementConfig().getProperty("javax.portlet-feed_url"));
		final String title = decode(tool.getPlacementConfig().getProperty("javax.portlet-portlet_title"));
		final String maxItems = tool.getPlacementConfig().getProperty("javax.portlet-max_items");

		final NewsData newsData = new NewsData();
		newsData.setUrl(url);
		newsData.setTitle(title);
		newsData.setMaxItems(maxItems);

		return newsData;
	}

	/**
	 * Decode a string. Simple RSS Portlet stores the encoded version.
	 *
	 * If encoding fails, the original is returned.
	 *
	 * @param string
	 * @return
	 */
	private String decode(final String string) {
		if (StringUtils.isBlank(string)) {
			return string;
		}
		try {
			return URLDecoder.decode(string, "UTF-8");
		} catch (final UnsupportedEncodingException e) {
			log.debug("Could not decode {}. Returning original.", string);
		}
		return string;
	}

	/**
	 * Get HTML representation
	 *
	 * @param data
	 * @return
	 */
	private String getAsHtml(final NewsData data) {
		final StringBuilder sb = new StringBuilder();

		if (StringUtils.isNotBlank(data.getTitle())) {
			sb.append("<h2>" + data.getTitle() + "</h2>");
		}
		sb.append("<p><a href=\"" + data.getUrl() + "\">" + data.getUrl() + "</a></p>");

		return sb.toString();
	}

	/**
	 * Data to be archived for the news tool
	 */
	public static class NewsData {

		@Getter
		@Setter
		private String url;

		@Getter
		@Setter
		private String title;

		@Getter
		@Setter
		private String maxItems;

	}
}
