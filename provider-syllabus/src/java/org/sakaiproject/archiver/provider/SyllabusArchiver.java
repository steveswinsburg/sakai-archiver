package org.sakaiproject.archiver.provider;

import java.util.Set;

import org.sakaiproject.api.app.syllabus.SyllabusAttachment;
import org.sakaiproject.api.app.syllabus.SyllabusData;
import org.sakaiproject.api.app.syllabus.SyllabusItem;
import org.sakaiproject.api.app.syllabus.SyllabusManager;
import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.spi.Archiveable;
import org.sakaiproject.archiver.util.Htmlifier;
import org.sakaiproject.archiver.util.Sanitiser;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.exception.TypeException;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link Archiveable} for various content tools
 */
@Slf4j
public class SyllabusArchiver implements Archiveable {

	private static final String TOOL_ID = "sakai.syllabus";

	@Setter
	private ContentHostingService contentHostingService;

	@Setter
	private SyllabusManager syllabusManager;

	@Setter
	private ArchiverService archiverService;

	public void init() {
		ArchiverRegistry.getInstance().register(TOOL_ID, this);
	}

	public void destroy() {
		ArchiverRegistry.getInstance().unregister(TOOL_ID);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void archive(final String archiveId, final String siteId, final boolean includeStudentContent) {

		// Get syllabus for site
		final SyllabusItem siteSyllabus = this.syllabusManager.getSyllabusItemByContextId(siteId);
		if (siteSyllabus == null) {
			log.error("No syllabus in site {}. The syllabus will not be archived.", siteId);
			return;
		}

		final String toolName = getToolName(siteId);

		// Get the data
		final Set<SyllabusData> syllabusSet = this.syllabusManager.getSyllabiForSyllabusItem(siteSyllabus);

		// Go through and archive each syllabus item
		for (final SyllabusData syllabus : syllabusSet) {

			final SimpleSyllabus simpleSyllabus = new SimpleSyllabus(syllabus.getTitle(), syllabus.getAsset());

			// archive the attachments
			final Set<SyllabusAttachment> syllabusAttachments = this.syllabusManager.getSyllabusAttachmentsForSyllabusData(syllabus);

			for (final SyllabusAttachment syllabusAttachment : syllabusAttachments) {
				byte[] syllabusAttachmentBytes;
				try {
					syllabusAttachmentBytes = this.contentHostingService.getResource(syllabusAttachment.getAttachmentId()).getContent();
					this.archiverService.archiveContent(archiveId, siteId, toolName, syllabusAttachmentBytes, syllabusAttachment.getName(),
							syllabus.getTitle() + "_attachments");
					addToAttachmentsHtml(Sanitiser.sanitise(syllabus.getTitle()) + "_attachments/",
							Sanitiser.sanitise(syllabusAttachment.getName()), simpleSyllabus);
				} catch (ServerOverloadException | PermissionException | IdUnusedException | TypeException e) {
					log.error("Error getting syllabus attachment " + syllabusAttachment.getName() + " in syllabus " + syllabus.getTitle());
					continue;
				}
			}

			if (!syllabusAttachments.isEmpty()) {
				finaliseAttachmentsHtml(simpleSyllabus);
			}

			// archive the syllabus as a html file
			final String htmlBody = getAsHtml(simpleSyllabus);
			final String html = Htmlifier.toHtml(htmlBody, this.archiverService.getSiteHeader(siteId, TOOL_ID));
			log.debug("Archive item metadata: " + html);
			this.archiverService.archiveContent(archiveId, siteId, toolName, html.getBytes(), simpleSyllabus.getTitle() + ".html");
		}
	}

	@Override
	public String getToolName(final String siteId) {
		return this.archiverService.getToolName(siteId, TOOL_ID);
	}

	/**
	 * Set the html string that contains a list of attachment hyperlinks
	 *
	 * @param attachmentLocation
	 * @param attachmentName
	 * @param simpleSyllabus
	 */
	private void addToAttachmentsHtml(final String attachmentLocation, final String attachmentName,
			final SimpleSyllabus simpleSyllabus) {

		final String attachmentHyperlink = "<li><a href=\"./" + attachmentLocation + attachmentName + "\">" + attachmentName
				+ "</a></li>";
		simpleSyllabus.setAttachments(simpleSyllabus.getAttachments() + attachmentHyperlink);
	}

	/**
	 * Finalise the attachments html string by surrounding it by unordered list tags
	 *
	 * @param simpleSyllabus
	 */
	private void finaliseAttachmentsHtml(final SimpleSyllabus simpleSyllabus) {

		simpleSyllabus
				.setAttachments("<ul style=\"list-style: none;padding-left:0;\">" + simpleSyllabus.getAttachments() + "</ul>");

	}

	/**
	 * Construct the syllabus html string
	 *
	 * @param simpleSyllabus
	 * @return syllabusBodyHtml
	 */
	private String getAsHtml(final SimpleSyllabus simpleSyllabus) {

		final StringBuilder sb = new StringBuilder();

		sb.append("<p>" + simpleSyllabus.getBody() + "</p>");

		if (simpleSyllabus.getAttachments().length() > 0) {
			sb.append("<p>" + simpleSyllabus.getAttachments() + "</p>");
		}

		return sb.toString();
	}

	/**
	 * Simplified helper class to represent metadata for an individual syllabus item in a site
	 */
	private static class SimpleSyllabus {

		@Getter
		@Setter
		private String title;

		@Getter
		@Setter
		private String body;

		@Getter
		@Setter
		private String attachments = "";

		public SimpleSyllabus(final String title, final String body) {
			this.title = title;
			this.body = body;
		}
	}

}
