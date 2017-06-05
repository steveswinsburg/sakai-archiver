package org.sakaiproject.archiver.provider;

import java.util.Date;
import java.util.List;

import org.sakaiproject.api.app.messageforums.DiscussionForum;
import org.sakaiproject.api.app.messageforums.ui.DiscussionForumManager;
import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.spi.Archiveable;
import org.sakaiproject.archiver.util.Jsonifier;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.exception.TypeException;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link Archiveable} for various content tools
 */
@Slf4j
public class ForumsArchiver implements Archiveable {

	private static final String TOOL_ID = "sakai.forums";
	
	@Setter
	protected DiscussionForumManager forumManager;

	public void init() {
		ArchiverRegistry.getInstance().register(TOOL_ID, this);
	}

	public void destroy() {
		ArchiverRegistry.getInstance().unregister(TOOL_ID);
	}

	@Setter
	private ContentHostingService contentHostingService;

	@Setter
	private ArchiverService archiverService;

	@SuppressWarnings("unchecked")
	@Override
	public void archive(final String archiveId, final String siteId, final String toolId, final boolean includeStudentContent) {

		List<DiscussionForum> forums = this.forumManager.getDiscussionForumsByContextId(siteId);
		
		for (DiscussionForum forum : forums) {
			
			SimpleForum simpleForum = new SimpleForum(forum);
			this.archiverService.archiveContent(archiveId, siteId, toolId, Jsonifier.toJson(simpleForum).getBytes(), forum.getTitle() + ".json");
			
			this.archiveAttachments(forum, archiveId, siteId, toolId);
			
		}
		
	}
	
	/**
	 * Get the attachments for this forum, and archive them
	 * @param assignment
	 * @param archiveId
	 * @param siteId
	 * @param toolId
	 */
	@SuppressWarnings("unchecked")
	private void archiveAttachments(DiscussionForum forum, final String archiveId, final String siteId, final String toolId) {
		List<Reference> forumAttachments = forum.getAttachments();
		for (Reference forumAttachment : forumAttachments) {
			try {
				archiveAttachment(forumAttachment, archiveId, siteId, toolId, forum.getTitle() + "/attachments");
			} catch (ServerOverloadException | IdUnusedException | TypeException | PermissionException e) {
				log.error("Error getting attachment for forum: {}", forum.getTitle());
				continue;
			} 
		}
	}
	
	/**
	 * Helper method to archive attachments
	 * 
	 * @param attachment
	 * @param archiveId
	 * @param siteId
	 * @param toolId
	 * @param title
	 * @throws ServerOverloadException
	 * @throws PermissionException
	 * @throws IdUnusedException
	 * @throws TypeException
	 */
	private void archiveAttachment(Reference attachment, String archiveId, String siteId, String toolId, String subdir) throws ServerOverloadException, PermissionException, IdUnusedException, TypeException {
		byte[] attachmentBytes = this.contentHostingService.getResource(attachment.getId()).getContent();
		this.archiverService.archiveContent(archiveId, siteId, toolId, attachmentBytes, 
				attachment.getProperties().getPropertyFormatted(attachment.getProperties().getNamePropDisplayName()), subdir);
	}


	
	private class SimpleForum {
		
		@Setter
		private String title;
		
		@Setter
		private String extendedDescription;
		
		@Setter
		private String shortDescription;
		
		@Setter
		private Boolean isLocked;
		
		@Setter
		private Boolean isModerated;
		
		@Setter
		private Boolean isPostFirst;
		
		@Setter
		private Date createdDate;
		
		@Setter
		private String createdBy;
		
		@Setter
		private Date openDate;
		
		@Setter
		private Date closeDate;
		
		@Setter
		private String assocGradebookItemName;
		
		@Setter
		private Boolean isDraft;
		
		@Setter
		private Date modifiedDate;

		@Setter
		private String modifiedBy;

		SimpleForum (DiscussionForum forum) {
			this.title = forum.getTitle();
			this.extendedDescription = forum.getExtendedDescription();
			this.shortDescription = forum.getShortDescription();
			this.isLocked = forum.getLocked();
			this.isModerated = forum.getModerated();
			this.isPostFirst = forum.getPostFirst();
			this.createdDate = forum.getCreated();
			this.createdBy = forum.getCreatedBy();
			this.assocGradebookItemName = forum.getDefaultAssignName();
			this.isDraft = forum.getDraft();
			this.modifiedDate = forum.getModified();
			this.modifiedBy = forum.getModifiedBy();
			
			// if availability is restricted, get open and close dates
			if (forum.getAvailabilityRestricted()) {
				this.openDate = forum.getOpenDate();
				this.closeDate = forum.getCloseDate();
			}
		}
		
	}
	

}
