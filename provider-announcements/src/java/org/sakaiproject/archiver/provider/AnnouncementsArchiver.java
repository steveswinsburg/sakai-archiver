package org.sakaiproject.archiver.provider;

import java.util.List;

import org.sakaiproject.announcement.api.AnnouncementChannel;
import org.sakaiproject.announcement.api.AnnouncementService;
import org.sakaiproject.archiver.api.Archiveable;
import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;

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

	@Override
	public void archive(final String archiveId, final String siteId, final String toolId, final boolean includeStudentContent) {
		
		try {
			AnnouncementChannel channel = announcementService.getAnnouncementChannel(announcementService.getAnnouncementReference(siteId).getId());
			List messages = channel.getMessages(null, true);
			
//			for (Message message : (List<Message>) messages) {
//				
//			}
			System.out.println(messages.toString());
			this.archiverService.archiveContent(archiveId, siteId, toolId, messages.toString().getBytes(), "announcements-export.json");
			
		} catch (IdUnusedException | PermissionException e) {
			log.debug("Failed to get announcements");
		}
		
	}

}
