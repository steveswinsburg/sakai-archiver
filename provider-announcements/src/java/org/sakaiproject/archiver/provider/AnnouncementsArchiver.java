package org.sakaiproject.archiver.provider;

import java.util.ArrayList;
import java.util.List;

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

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link Archiveable} for the announcements tool
 */

@Slf4j
public class AnnouncementsArchiver implements Archiveable {

	private static final String TOOL_ID = "sakai.announcements";
	private static final String TOOL_NAME = "Announcements";

	public void init() {
		ArchiverRegistry.getInstance().register(TOOL_ID, this);
	}

	public void destroy() {
		ArchiverRegistry.getInstance().unregister(TOOL_ID);
	}

	@Setter
	private ArchiverService archiverService;

	@Setter
	private AnnouncementService announcementService;

	@Setter
	private ContentHostingService contentHostingService;

	private String attachmentsHtml;

	@Override
	public void archive(final String archiveId, final String siteId, final boolean includeStudentContent) {

		try {

			// Get the announcements for this site
			final String channelRef = this.announcementService.channelReference(siteId, SiteService.MAIN_CONTAINER);
			final List<Message> announcements = new ArrayList<>();
			announcements.addAll(this.announcementService.getMessages(channelRef, null, 0, true, false, false));

			// Go through each announcement and save the data we are interested in
			for (final Message message : announcements) {
				this.attachmentsHtml = "";
				final AnnouncementMessage announcement = (AnnouncementMessage) message;

				// archive the attachments for this announcement
				final List<Reference> attachments = announcement.getAnnouncementHeader().getAttachments();
				archiveAttachments(attachments, announcement, archiveId, siteId, TOOL_NAME);
				if (!attachments.isEmpty()) {
					finaliseAttachmentsHtml();
				}

				// convert to html
				final String htmlBody = getAsHtml(announcement);
				final String fileContents = Htmlifier.toHtml(htmlBody, this.archiverService.getSiteHeader(siteId, TOOL_ID));

				// Save this announcement
				log.debug("Announcement data: " + fileContents);
				this.archiverService.archiveContent(archiveId, siteId, TOOL_NAME, fileContents.getBytes(),
						announcement.getAnnouncementHeader().getSubject() + ".html");

			}
		} catch (final PermissionException e) {
			log.error("Failed to get announcements", e);
		}
	}

	private String getAsHtml(final AnnouncementMessage announcement) {
		final StringBuilder sb = new StringBuilder();

		sb.append("<h2>" + announcement.getAnnouncementHeader().getSubject() + "</h2>");
		sb.append("<p>" + announcement.getHeader().getFrom().getDisplayName()
				+ String.format(" (%s) ", announcement.getHeader().getDate().getDisplay())
				+ "</p");
		sb.append("<p>" + announcement.getBody() + "</p>");
		sb.append("<p>" + this.attachmentsHtml + "</p>");

		return sb.toString();
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
	private void archiveAttachments(final List<Reference> attachments, final AnnouncementMessage announcement, final String archiveId,
			final String siteId, final String toolId) throws PermissionException {

		for (final Reference attachment : attachments) {
			byte[] attachmentBytes;
			try {
				attachmentBytes = this.contentHostingService.getResource(attachment.getId()).getContent();
				final String attachmentName = attachment.getProperties()
						.getPropertyFormatted(attachment.getProperties().getNamePropDisplayName());
				this.archiverService.archiveContent(archiveId, siteId, toolId, attachmentBytes,
						attachmentName, announcement.getAnnouncementHeader().getSubject() + " (attachments)");
				addToAttachmentsHtml(announcement.getAnnouncementHeader().getSubject() + " (attachments)/", attachmentName);

			} catch (ServerOverloadException | IdUnusedException | TypeException e) {
				log.error("Error getting attachment for announcement: " + announcement.getAnnouncementHeader().getSubject());
				continue;
			}
		}
	}

	/**
	 * Set the html string that contains a list of attachment hyperlinks
	 *
	 * @param attachmentLocation
	 * @param attachmentName
	 * @param simpleArchiveItem
	 */
	private void addToAttachmentsHtml(final String attachmentLocation, final String attachmentName) {

		final String attachmentHyperlink = "<li><a href=\"./" + attachmentLocation + attachmentName + "\">" + attachmentName
				+ "</a></li>";
		this.attachmentsHtml += attachmentHyperlink;
	}

	/**
	 * Finalise the attachments html string by surrounding it by unordered list tags
	 *
	 * @param simpleArchiveItem
	 */
	private void finaliseAttachmentsHtml() {

		this.attachmentsHtml = "<ul style=\"list-style: none;padding-left:0;\">" + this.attachmentsHtml + "</ul>";
	}
}
