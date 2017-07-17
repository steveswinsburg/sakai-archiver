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
	private static final String TOOL_NAME = "Syllabus";

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

		// Get the data
		final Set<SyllabusData> syllabusSet = this.syllabusManager.getSyllabiForSyllabusItem(siteSyllabus);

		// Go through and archive each syllabus item
		for (final SyllabusData syllabus : syllabusSet) {
			final SimpleSyllabus archiveItem = new SimpleSyllabus(syllabus.getTitle(), syllabus.getAsset());
			final String htmlArchiveItem = Htmlifier.addSiteHeader(Htmlifier.toHtml(archiveItem),
					this.archiverService.getSiteHeader(siteId, TOOL_ID));
			log.debug("Archive item metadata: " + htmlArchiveItem);
			this.archiverService.archiveContent(archiveId, siteId, TOOL_NAME, htmlArchiveItem.getBytes(), archiveItem.getTitle() + ".html");

			// get the attachments
			final Set<SyllabusAttachment> syllabusAttachments = this.syllabusManager.getSyllabusAttachmentsForSyllabusData(syllabus);

			for (final SyllabusAttachment syllabusAttachment : syllabusAttachments) {
				byte[] syllabusAttachmentBytes;
				try {
					syllabusAttachmentBytes = this.contentHostingService.getResource(syllabusAttachment.getAttachmentId()).getContent();
					this.archiverService.archiveContent(archiveId, siteId, TOOL_NAME, syllabusAttachmentBytes, syllabusAttachment.getName(),
							syllabus.getTitle() + " (attachments)");
				} catch (ServerOverloadException | PermissionException | IdUnusedException | TypeException e) {
					log.error("Error getting syllabus attachment " + syllabusAttachment.getName() + " in syllabus " + syllabus.getTitle());
					continue;
				}
			}
		}
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

		public SimpleSyllabus(final String title, final String body) {
			this.title = title;
			this.body = body;
		}
	}

}
