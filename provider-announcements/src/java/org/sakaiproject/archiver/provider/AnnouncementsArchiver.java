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

				final SimpleAnnouncement simpleAnnouncement = new SimpleAnnouncement(announcement);

				// archive the attachments for this announcement
				final List<Reference> attachments = announcement.getAnnouncementHeader().getAttachments();
				archiveAttachments(attachments, simpleAnnouncement, archiveId, siteId,
						toolId);
				if (attachments.size() > 0) {
					finaliseAttachmentsHtml(simpleAnnouncement);
				}

				// convert to html
				final String fileContents = Htmlifier.toHtml(simpleAnnouncement);

				// Save this announcement
				log.debug("Announcement data: " + fileContents);
				this.archiverService.archiveContent(archiveId, siteId, toolId, fileContents.getBytes(),
						simpleAnnouncement.getTitle() + ".html");

			}
		} catch (final PermissionException e) {
			log.error("Failed to get announcements");
		}
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
	private void archiveAttachments(final List<Reference> attachments, final SimpleAnnouncement simpleAnnouncement, final String archiveId,
			final String siteId,
			final String toolId) throws PermissionException {

		for (final Reference attachment : attachments) {
			byte[] attachmentBytes;
			try {
				attachmentBytes = this.contentHostingService.getResource(attachment.getId()).getContent();
				final String attachmentName = attachment.getProperties()
						.getPropertyFormatted(attachment.getProperties().getNamePropDisplayName());
				this.archiverService.archiveContent(archiveId, siteId, toolId, attachmentBytes,
						attachmentName, simpleAnnouncement.getTitle() + " (attachments)");
				addToAttachmentsHtml(simpleAnnouncement.getTitle() + " (attachments)/", attachmentName, simpleAnnouncement);

			} catch (ServerOverloadException | IdUnusedException | TypeException e) {
				log.error("Error getting attachment for announcement: " + simpleAnnouncement.getTitle());
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
	private void addToAttachmentsHtml(final String attachmentLocation, final String attachmentName,
			final SimpleAnnouncement simpleAnnouncement) {

		final String attachmentHyperlink = "<li><a href=\"./" + attachmentLocation + attachmentName + "\">" + attachmentName
				+ "</a></li>";
		simpleAnnouncement.setAttachments(simpleAnnouncement.getAttachments() + attachmentHyperlink);
	}

	/**
	 * Finalise the attachments html string by surrounding it by unordered list tags
	 *
	 * @param simpleArchiveItem
	 */
	private void finaliseAttachmentsHtml(final SimpleAnnouncement simpleAnnouncement) {

		simpleAnnouncement
				.setAttachments("<ul style=\"list-style: none;padding-left:0;\">" + simpleAnnouncement.getAttachments() + "</ul>");

	}

	/**
	 * Simplified helper class to represent individual announcement in a site
	 */
	private class SimpleAnnouncement {

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

		@Getter
		@Setter
		private String attachments = "";

		public SimpleAnnouncement(final AnnouncementMessage announcement) {
			setTitle(announcement.getAnnouncementHeader().getSubject());
			setCreatedBy(announcement.getHeader().getFrom().getDisplayName());
			setCreatedOn(announcement.getHeader().getDate().getDisplay());
			setBody(announcement.getBody());
		}

	}

}
