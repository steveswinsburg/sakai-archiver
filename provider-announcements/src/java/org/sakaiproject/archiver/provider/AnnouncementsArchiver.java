package org.sakaiproject.archiver.provider;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.sakaiproject.announcement.api.AnnouncementMessage;
import org.sakaiproject.announcement.api.AnnouncementService;
import org.sakaiproject.archiver.api.Archiveable;
import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.util.Jsonifier;
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
			String channelRef = this.announcementService.channelReference(siteId, SiteService.MAIN_CONTAINER);
			List<Message> announcements = new ArrayList<Message>();
			announcements.addAll(this.announcementService.getMessages(channelRef, null, 0, true, false, false));
			
			// Go through each announcement and save the data we are interested in
			for (Message message : announcements) {
				AnnouncementMessage announcement = (AnnouncementMessage) message;
				
				// Save the metadata for this announcement
				ArchiveItem archiveItem = new ArchiveItem();
				archiveItem.setTitle(announcement.getAnnouncementHeader().getSubject());
				archiveItem.setCreatedBy(announcement.getHeader().getFrom().getDisplayName());
				archiveItem.setCreatedOn(new Date(announcement.getHeader().getDate().getTime()));
				String jsonArchiveItem = Jsonifier.toJson(archiveItem);
				log.debug("Announcement metadata: " + jsonArchiveItem);
				this.archiverService.archiveContent(archiveId, siteId, toolId, jsonArchiveItem.getBytes(), archiveItem.getTitle() + " (metadata).json");
				
				// Save the body of the announcement
				String announcementBody = announcement.getBody();
				log.debug("Announcement body: " + announcementBody);
				this.archiverService.archiveContent(archiveId, siteId, toolId, announcementBody.getBytes(), archiveItem.getTitle() + " (content).html");
				
				// get the attachments
				for (Reference attachment : announcement.getAnnouncementHeader().getAttachments()) {
					byte[] attachmentBytes;
					try {
						attachmentBytes = this.contentHostingService.getResource(attachment.getId()).getContent();
						this.archiverService.archiveContent(archiveId, siteId, toolId, attachmentBytes, 
								attachment.getProperties().getPropertyFormatted(attachment.getProperties().getNamePropDisplayName()), 
								archiveItem.getTitle() + " (attachments)");
					} catch (ServerOverloadException | IdUnusedException | TypeException e) {
						log.debug("Error getting attachment for announcement: " + archiveItem.getTitle());
						continue;
					}
					
				}
			}
		} catch (PermissionException e) {
			log.debug("Failed to get announcements");
		}
	}
	
	/**
	 * Simplified helper class to represent metadata for an individual announcement in a site
	 */
	private static class ArchiveItem {
		
		@Getter @Setter private String title;
		@Getter @Setter private String createdBy;
		@Getter @Setter private Date createdOn;
		@Getter @Setter private String channel;
	}

}
