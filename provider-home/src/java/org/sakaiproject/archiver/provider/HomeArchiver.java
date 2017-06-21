package org.sakaiproject.archiver.provider;

import org.apache.commons.lang3.StringEscapeUtils;
import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.spi.Archiveable;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link Archiveable} for the home tool
 *
 * @author Steve Swinsburg(steve.swinsburg@gmail.com)
 * @since 12.0
 */
@Slf4j
public class HomeArchiver implements Archiveable {

	private static final String TOOL_ID = "sakai.iframe.site";

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
	public void archive(final String archiveId, final String siteId, final String toolId, final boolean includeStudentContent) {

		Site site;
		try {
			site = this.siteService.getSite(siteId);
		} catch (final IdUnusedException e) {
			log.error("Invalid site. Cannot lookup home info.");
			return;
		}

		final String description = site.getHtmlDescription();
		final String fileContents = createHtmlFileContents(description);

		this.archiverService.archiveContent(archiveId, siteId, TOOL_ID, fileContents.getBytes(), "index.html");
	}

	/**
	 * Turn the description into HTML
	 *
	 * @param data
	 * @return
	 */
	private String createHtmlFileContents(final String data) {
		return StringEscapeUtils.unescapeHtml4(data);
	}

}
