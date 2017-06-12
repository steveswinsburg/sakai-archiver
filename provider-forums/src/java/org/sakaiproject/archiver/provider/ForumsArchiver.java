package org.sakaiproject.archiver.provider;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.sakaiproject.api.app.messageforums.DiscussionForum;
import org.sakaiproject.api.app.messageforums.DiscussionTopic;
import org.sakaiproject.api.app.messageforums.Message;
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

import lombok.Getter;
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

		List<DiscussionForum> forums = this.forumManager.getDiscussionForumsWithTopics(siteId);
		
		for (DiscussionForum forum : forums) {
			
			SimpleForum simpleForum = new SimpleForum(forum);
			this.archiverService.archiveContent(archiveId, siteId, toolId, Jsonifier.toJson(simpleForum).getBytes(), forum.getTitle() + ".json", forum.getTitle());
			
			// TODO: Different with student content
			//this.archiveAttachments(forum, archiveId, siteId, toolId);
			
			this.archiveTopics(forum, archiveId, siteId, toolId);
			
		}
	}
	
	/**
	 * Archive the topics within a forum
	 * 
	 * @param forum
	 * @param archiveId
	 * @param siteId
	 * @param toolId
	 */
	private void archiveTopics(DiscussionForum forum, final String archiveId, final String siteId, final String toolId) {
		
		List<DiscussionTopic> topics = forum.getTopics();
		for (DiscussionTopic topic : topics) {
			SimpleTopic simpleTopic = new SimpleTopic(topic);
			this.archiverService.archiveContent(archiveId, siteId, toolId, Jsonifier.toJson(simpleTopic).getBytes(), 
					"topic-metadata.json", forum.getTitle() + "/topics/" + topic.getTitle());
			
			//TODO: Topic attachments
			
			// Archive the messages within a topic
			String folderStructure = forum.getTitle() + "/topics/" + topic.getTitle();
			 
			List<Message> messages = topic.getMessages();
			for (Message message : messages) {
				
				// Find the top level message
				if (message.getInReplyTo() == null) {
					SimpleMessage topLevelMessage = new SimpleMessage(message);
					// Set message replies
					this.setMessageReplies(topLevelMessage, message, messages);
					
					// Archive the messages
					this.archiverService.archiveContent(archiveId, siteId, toolId, Jsonifier.toJson(topLevelMessage).getBytes(), 
							message.getTitle() + ".json", folderStructure);
				}
				
				//TODO: Message attachments
				if (message.getHasAttachments()) {
					
				}
			}
		}
		
	}
	
	/**
	 * Add any replies to each SimpleMessage object
	 * 
	 * @param simpleTopMessage
	 * @param topMessage
	 * @param messages
	 */
	private void setMessageReplies(SimpleMessage simpleTopMessage, Message topMessage, List<Message> messages) {
		
		for (Message message : messages) {
			if (message.getInReplyTo() == topMessage) {
				List<SimpleMessage> replies = simpleTopMessage.getReplies();
				SimpleMessage thisMessage = new SimpleMessage(message);
				replies.add(thisMessage);
				
				// Recursively set the replies for this inner message
				this.setMessageReplies(thisMessage, message, messages);
			}
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
	
	/**
	 *  Simplified Message class
	 */
	private class SimpleMessage {

		@Setter
		private Long messageId;
		
		@Setter
		private String title;
		
		@Setter
		private String body;
		
		@Setter
		private Date lastModified;
		
		@Setter
		private String authoredBy;
		
		@Setter
		private String authorId;
		
		@Setter
		private Long replyTo;
		
		@Setter
		private Date createdOn;
		
		@Setter
		private boolean isDraft;
		
		@Setter
		private boolean isDeleted;

		@Setter
		private String modifiedBy;
		
		@Setter @Getter
		private List<SimpleMessage> replies = new ArrayList<SimpleMessage>();
		
		public SimpleMessage(Message message) {
			
			this.messageId = message.getId();
			this.title = message.getTitle();
			this.body = message.getBody();
			this.lastModified = message.getModified();
			this.authoredBy = message.getAuthor();
			this.authorId = message.getAuthorId();
			this.isDraft = message.getDraft();
			this.isDeleted = message.getDeleted();
			this.createdOn = message.getCreated();
			this.modifiedBy = message.getModifiedBy();
			
			Message parent = message.getInReplyTo();
			if (parent != null) {
				this.replyTo = parent.getId();
			} 
		}
	}

	
	/**
	 * Simplified Topic class
	 */
	private class SimpleTopic {
		
		@Setter
		private Long topicId;
		
		@Setter
		private String title;
		
		@Setter
		private Date createdDate;
		
		@Setter
		private String creator;
		
		@Setter
		private Date modifiedDate;
		
		@Setter
		private String modifier;
		
		@Setter
		private Boolean isLocked;

		@Setter
		private Boolean isPostFirst;

		@Setter
		private String assocGradebookItemName;

		@Setter
        private Date openDate;

		@Setter
        private Date closeDate;
	        
		public SimpleTopic (DiscussionTopic topic) {
			this.topicId = topic.getId();
			this.title = topic.getTitle();
			this.createdDate = topic.getCreated();
			this.creator = topic.getCreatedBy();
			this.modifiedDate = topic.getModified();
			this.modifier = topic.getModifiedBy();
			this.isLocked = topic.getLocked();
			this.isPostFirst = topic.getPostFirst();
			this.assocGradebookItemName = topic.getDefaultAssignName();
			
			// if availability is restricted, get open and close dates
			if (topic.getAvailabilityRestricted()) {
				this.openDate = topic.getOpenDate();
				this.closeDate = topic.getCloseDate();
			}
		}
	}

	/**
	 * Simplified Forum task
	 */
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
