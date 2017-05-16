package org.sakaiproject.archiver.provider;

import java.util.Date;
import java.util.Set;

import org.sakaiproject.api.app.syllabus.SyllabusAttachment;
import org.sakaiproject.api.app.syllabus.SyllabusData;
import org.sakaiproject.api.app.syllabus.SyllabusItem;
import org.sakaiproject.api.app.syllabus.SyllabusManager;
import org.sakaiproject.archiver.api.Archiveable;
import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.util.Jsonifier;
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

	private static final String HOME_TOOL = "sakai.syllabus";

	public void init() {
		ArchiverRegistry.getInstance().register(HOME_TOOL, this);
	}

	public void destroy() {
		ArchiverRegistry.getInstance().unregister(HOME_TOOL);
	}

	@Setter
	private ContentHostingService contentHostingService;
	
	@Setter
	private SyllabusManager syllabusManager;

	@Setter
	private ArchiverService archiverService;

	@SuppressWarnings("unchecked")
	@Override
	public void archive(final String archiveId, final String siteId, final String toolId, final boolean includeStudentContent) {

		log.info("Archiving {}", toolId);

		// Get syllabus
		SyllabusItem siteSyllabus = this.syllabusManager.getSyllabusItemByContextId(siteId);

		// Get the data
		Set<SyllabusData> syllabusSet = this.syllabusManager.getSyllabiForSyllabusItem(siteSyllabus);

		// Go through and archive each syllabus item
		ArchiveItem archiveItem = new ArchiveItem();
		for (SyllabusData syllabus : syllabusSet) {
			log.debug("Collecting " + syllabus.getTitle());
			archiveItem.setTitle(syllabus.getTitle());
			archiveItem.setData(syllabus.getAsset());
			archiveItem.setStartDate(syllabus.getStartDate());
			archiveItem.setEndDate(syllabus.getEndDate());

			//get the attachments
			Set<SyllabusAttachment> syllabusAttachments = this.syllabusManager.getSyllabusAttachmentsForSyllabusData(syllabus);

			for (SyllabusAttachment syllabusAttachment : syllabusAttachments) {
				byte[] syllabusAttachmentBytes;
				try {
					syllabusAttachmentBytes = this.contentHostingService.getResource(syllabusAttachment.getAttachmentId()).getContent();
					this.archiverService.archiveContent(archiveId, siteId, "sakai.syllabus", syllabusAttachmentBytes, syllabusAttachment.getName(), syllabus.getTitle());
					log.debug("Attachment:  " +syllabusAttachment.getName() + " Sub-directory: " + syllabus.getTitle());
				} catch (ServerOverloadException | PermissionException | IdUnusedException | TypeException e) {
					e.printStackTrace();
				}
			}
			log.debug("JSON: " +Jsonifier.toJson(archiveItem));
			this.archiverService.archiveContent(archiveId, siteId, "sakai.syllabus", Jsonifier.toJson(archiveItem).getBytes(), archiveItem.getTitle() + ".json");
		}
		

	}
	
	/**
	 * Simplified helper class to represent an individual syllabus item in a site
	 */
	public static class ArchiveItem {
		
		@Getter @Setter
		private String title;
		
		@Getter @Setter
		private String data;
		
		@Getter @Setter
		private Date startDate;
		
		@Getter @Setter
		private Date endDate;
	}
}
