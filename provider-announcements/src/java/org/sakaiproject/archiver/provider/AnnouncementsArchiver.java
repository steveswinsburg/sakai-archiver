package org.sakaiproject.archiver.provider;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.sakaiproject.announcement.api.AnnouncementMessage;
import org.sakaiproject.announcement.api.AnnouncementService;
import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.spi.Archiveable;
import org.sakaiproject.archiver.util.Htmlifier;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.exception.TypeException;
import org.sakaiproject.message.api.Message;
import org.sakaiproject.site.api.SiteService;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link Archiveable} for the announcements tool
 */

@Slf4j
public class AnnouncementsArchiver implements Archiveable {

	private static final String ANNOUNCEMENTS_TOOL = "sakai.announcements";

	public void init() {
		ArchiverRegistry.getInstance().register(ANNOUNCEMENTS_TOOL, this);
	}

	public void destroy() {
		ArchiverRegistry.getInstance().unregister(ANNOUNCEMENTS_TOOL);
	}

	@Setter
	private ArchiverService archiverService;

	@Setter
	private AnnouncementService announcementService;

	@Setter
	private ContentHostingService contentHostingService;

	private String attachmentHyperlinksList = "";

	@Override
	public void archive(final String archiveId, final String siteId, final String toolId, final boolean includeStudentContent) {

		try {

			// Get the announcements for this site
			final String channelRef = this.announcementService.channelReference(siteId, SiteService.MAIN_CONTAINER);
			final List<Message> announcements = new ArrayList<>();
			announcements.addAll(this.announcementService.getMessages(channelRef, null, 0, true, false, false));

			// Go through each announcement and save the data we are interested in
			for (final Message message : announcements) {
				final AnnouncementMessage announcement = (AnnouncementMessage) message;

				final ArchiveItem archiveItem = createArchiveItem(announcement);

				// archive the attachments for this announcement
				final List<Reference> attachments = announcement.getAnnouncementHeader().getAttachments();
				archiveAttachments(attachments, archiveItem.getTitle(), archiveId, siteId,
						toolId);

				// convert to html and append any attachments
				String fileContents = Htmlifier.toHtml(archiveItem);

				if (attachments.size() > 0) {
					fileContents = includeAttachmentsInHtml(fileContents);
				}

				// Save this announcement
				log.debug("Announcement data: " + fileContents);
				this.archiverService.archiveContent(archiveId, siteId, toolId, fileContents.getBytes(), archiveItem.getTitle() + ".html");

			}
		} catch (final PermissionException e) {
			log.error("Failed to get announcements");
		}
	}

	/**
	 * Add attachment links to the last row of the table
	 *
	 * @param fileContents
	 * @return updated fileContents string
	 */
	private String includeAttachmentsInHtml(final String fileContents) {
		final Document doc = Jsoup.parse(fileContents);
		final Element table = doc.select("table").first();
		final Element lastRow = table.select("tr").last();
		lastRow.appendElement("tr");
		lastRow.append("<tr><td>Attachments</td><td><ul></ul></td></tr>");
		final Elements attachmentsList = doc.select("ul");
		attachmentsList.append(this.attachmentHyperlinksList);

		return doc.html();
	}

	/**
	 * Append each attachment as a hyperlink list item to the final attachments html string
	 * 
	 * @param attachmentName
	 * @param attachmentLocation
	 */
	private void addToAttachmentList(final String attachmentName, final String attachmentLocation) {

		final String attachmentHyperlink = "<li><a href=\"" + attachmentLocation + "/" + attachmentName + "\">" + attachmentName
				+ "</a></li>";
		this.attachmentHyperlinksList = this.attachmentHyperlinksList + attachmentHyperlink;

	}

	/**
	 * Archive the attachments associated with an announcement
	 *
	 * @param attachments
	 * @param title
	 * @param archiveId
	 * @param siteId
	 * @param toolId
	 * @throws PermissionException
	 */
	private void archiveAttachments(final List<Reference> attachments, final String title, final String archiveId, final String siteId,
			final String toolId) throws PermissionException {

		for (final Reference attachment : attachments) {
			byte[] attachmentBytes;
			try {
				attachmentBytes = this.contentHostingService.getResource(attachment.getId()).getContent();
				final String attachmentName = attachment.getProperties()
						.getPropertyFormatted(attachment.getProperties().getNamePropDisplayName());
				this.archiverService.archiveContent(archiveId, siteId, toolId, attachmentBytes,
						attachmentName, title + " (attachments)");
				addToAttachmentList(attachmentName, title + " (attachments)");

			} catch (ServerOverloadException | IdUnusedException | TypeException e) {
				log.error("Error getting attachment for announcement: " + title);
				continue;
			}
		}
	}

	/**
	 * Build the ArchiveItem for an announcement
	 *
	 * @param announcement
	 * @return the archive item to be saved
	 */
	private ArchiveItem createArchiveItem(final AnnouncementMessage announcement) {

		final ArchiveItem archiveItem = new ArchiveItem();
		archiveItem.setTitle(announcement.getAnnouncementHeader().getSubject());
		archiveItem.setCreatedBy(announcement.getHeader().getFrom().getDisplayName());
		archiveItem.setCreatedOn(announcement.getHeader().getDate().getDisplay());
		archiveItem.setBody(announcement.getBody());

		return archiveItem;
	}

	/**
	 * Simplified helper class to represent individual announcement in a site
	 */
	private class ArchiveItem {

		@Getter
		@Setter
		private String title;

		@Getter
		@Setter
		private String body;

		@Getter
		@Setter
		private String createdBy;

		@Getter
		@Setter
		private String createdOn;

	}

}
