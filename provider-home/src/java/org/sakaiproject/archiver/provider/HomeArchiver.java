package org.sakaiproject.archiver.provider;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.spi.Archiveable;
import org.sakaiproject.archiver.util.Htmlifier;
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

	private String toolName;

	@Override
	public void archive(final String archiveId, final String siteId, final boolean includeStudentContent) {

		Site site;
		try {
			site = this.siteService.getSite(siteId);
		} catch (final IdUnusedException e) {
			log.error("Invalid site. Cannot lookup home info.");
			return;
		}

		this.toolName = getToolName(siteId);

		// get the html for the home frame
		final String description = site.getHtmlDescription();
		final String originalHtml = createHtmlFileContents(description);

		// archive any images, alter html to point to the archived images
		final String htmlWithLocalImages = archiveImages(originalHtml, archiveId, siteId);

		// add header to html
		final String finalHtml = Htmlifier.toHtml(htmlWithLocalImages, this.archiverService.getSiteHeader(siteId, TOOL_ID));

		// archive the home frame html
		this.archiverService.archiveContent(archiveId, siteId, this.toolName, finalHtml.getBytes(), "index.html");
	}

	@Override
	public String getToolName(final String siteId) {
		return this.archiverService.getToolName(siteId, TOOL_ID);
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

	/**
	 * Archive images that are in the home frame and alter the home frame html to point to these locally saved images
	 *
	 * @param originalHtml
	 * @param archiveId
	 * @param siteId
	 * @return updated html
	 */
	private String archiveImages(final String originalHtml, final String archiveId, final String siteId) {

		final Document doc = Jsoup.parse(originalHtml);
		final List<Element> imageElements = doc.select("img");

		for (final Element e : imageElements) {
			try {

				// get the image bytes
				final String imageSource = e.absUrl("src");
				final byte[] bytes = Jsoup.connect(imageSource).ignoreContentType(true).execute().bodyAsBytes();

				// get the file name
				final String filename = getFileName(imageSource);

				// set a maximum image width
				e.attr("style", "max-width: 100%");
				e.attr("height", "auto");

				// archive the image
				this.archiverService.archiveContent(archiveId, siteId, this.toolName, bytes, filename, "/images");

				// change the src for this image in the html
				e.attr("src", "images/" + filename);

			} catch (final IOException e1) {
				log.debug("Error when saving image from src: " + e.absUrl("src"));
				continue;
			}
		}

		return doc.body().toString();
	}

	/**
	 * Assign a filename for this image.
	 *
	 * If there is a file extension in the url, the filename will be the text from the last backslash to the file extension. Otherwise, a
	 * random UUID will be assigned.
	 *
	 * @param imageSource url string
	 * @return filename string
	 */
	private String getFileName(final String imageSource) {
		final Pattern pattern = Pattern.compile(
				"/([^\\/]*\\.png)|([^\\/]*\\.jpeg)|([^\\/]*\\.tif)|([^\\/]*\\.jpg)|([^\\/]*\\.gif)|([^\\/]*\\.bmp)|([^\\/]*\\.svg)");
		final Matcher matcher = pattern.matcher(imageSource);
		String filename;
		if (matcher.find()) {
			filename = matcher.group();
		} else { // none of the file extensions tested were present in the url
			filename = UUID.randomUUID().toString();
		}

		return filename;
	}

}
