package org.sakaiproject.archiver.provider;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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

		// get the html for the home frame
		final String description = site.getHtmlDescription();
		final String fileContents = createHtmlFileContents(description);

		// save any images
		final Document doc = Jsoup.parse(fileContents);
		final List<Element> imageElements = doc.select("img");
		saveImages(imageElements, archiveId, siteId, toolId);
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

	private void saveImages(final List<Element> imageElements, final String archiveId, final String siteId, final String toolId) {

		for (final Element e : imageElements) {
			try {

				// Get the image bytes
				final String imageSrc = e.absUrl("src");
				final byte[] bytes = Jsoup.connect(imageSrc).ignoreContentType(true).execute().bodyAsBytes();

				// Get the name of the image. Not completely reliable.
				final int nameIndex = imageSrc.lastIndexOf("/") + 1;
				String name;
				if (nameIndex > 0) {
					name = imageSrc.substring(nameIndex, imageSrc.length());
				} else {
					name = "unnamed_image.png";
				}

				// Remove any query text
				final int queryIndex = name.lastIndexOf("?");
				if (queryIndex > 0) {
					name = name.substring(0, queryIndex);
				}

				this.archiverService.archiveContent(archiveId, siteId, toolId, bytes, name);
			} catch (final IOException e1) {
				log.debug("Error when saving image from src: " + e.absUrl("src"));
				continue;
			}
		}
	}

}
