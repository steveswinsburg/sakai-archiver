package org.sakaiproject.archiver.provider;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.sakaiproject.api.app.messageforums.Attachment;
import org.sakaiproject.api.app.messageforums.DiscussionForum;
import org.sakaiproject.api.app.messageforums.DiscussionTopic;
import org.sakaiproject.api.app.messageforums.Message;
import org.sakaiproject.api.app.messageforums.Topic;
import org.sakaiproject.api.app.messageforums.ui.DiscussionForumManager;
import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.spi.Archiveable;
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

		final List<DiscussionForum> forums = this.forumManager.getDiscussionForumsWithTopics(siteId);

		for (final DiscussionForum forum : forums) {
			// Archive the forum, including topics (and messages if includeStudentContent is true)
			archiveForum(forum, archiveId, siteId, toolId, includeStudentContent);

			// Archive the attachments for this forum
			archiveAttachments(forum.getAttachments(), forum.getTitle() + "/forum-attachments/", archiveId,
					siteId, toolId);
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
	@SuppressWarnings("unchecked")
	private void archiveForum(final DiscussionForum forum, final String archiveId, final String siteId, final String toolId,
			final boolean includeStudentContent) {

		// Set up the simple forum object
		final SimpleForum simpleForum = new SimpleForum(forum);

		// Initialise the array to hold each topic for this forum
		final List<SimpleTopic> simpleTopics = new ArrayList<SimpleTopic>();

		final List<DiscussionTopic> topics = forum.getTopics();
		for (final DiscussionTopic topic : topics) {
			final SimpleTopic simpleTopic = new SimpleTopic(topic);

			// Add this topic to the forum's topic array
			simpleTopics.add(simpleTopic);

			// Set up the folder structure for saving attachments and messages
			final String folderStructure = forum.getTitle() + "/topics/" + topic.getTitle();

			// Archive the attachments for this topic
			archiveAttachments(topic.getAttachments(), folderStructure + "/topic-attachments/", archiveId, siteId,
					toolId);

			// Archive the messages within a topic, if we want student content
			if (includeStudentContent) {

				final List<Message> messages = topic.getMessages();
				for (final Message message : messages) {

					// Find the top level message
					if (message.getInReplyTo() == null) {
						final SimpleMessage topLevelMessage = new SimpleMessage(message);

						// Set message replies
						setMessageReplies(topLevelMessage, messages);

						// Archive the messages
						this.archiverService.archiveContent(archiveId, siteId, toolId, Jsonifier.toJson(topLevelMessage).getBytes(),
								message.getTitle() + ".json", folderStructure);
					}

					// Archive the attachments for this message
					if (message.getHasAttachments()) {

						// It is unfortunately necessary to get the attachments for a message by first getting the topic populated with
						// the message attachments, and adding the attachments to the message. See doc on setAttachments method.
						final Topic topicWithMessageAttachments = this.forumManager.getTopicByIdWithMessagesAndAttachments(topic.getId());
						setAttachments(message, topicWithMessageAttachments.getMessages());

						archiveAttachments(message.getAttachments(),
								folderStructure + "/topic-attachments/message-attachments/message-" + message.getId(), archiveId, siteId,
								toolId);
					}
				}
			}
		}

		// Now that all the topics are set, archive the forum
		simpleForum.setTopics(simpleTopics);
		this.archiverService.archiveContent(archiveId, siteId, toolId, Jsonifier.toJson(simpleForum).getBytes(), "forum-metadata.json",
				forum.getTitle());
	}

	/**
	 * Add any replies to each SimpleMessage object
	 *
	 * @param simpleTopMessage The first message posted in a topic, as a SimpleMessage
	 * @param messages The full list of messages for this topic
	 */
	private void setMessageReplies(final SimpleMessage simpleTopMessage, final List<Message> messages) {

		for (final Message message : messages) {
			if ((message.getInReplyTo() != null) && (message.getInReplyTo().getId() == simpleTopMessage.getMessageId())) {
				final List<SimpleMessage> replies = simpleTopMessage.getReplies();
				final SimpleMessage thisMessage = new SimpleMessage(message);
				replies.add(thisMessage);

				// Recursively set the replies for this inner message
				setMessageReplies(thisMessage, messages);
			}
		}
	}

	/**
	 * Archive a list of attachments
	 *
	 * @param assignment
	 * @param archiveId
	 * @param siteId
	 * @param toolId
	 */
	private void archiveAttachments(final List<Attachment> attachments, final String subdir,
			final String archiveId, final String siteId, final String toolId) {
		for (final Attachment attachment : attachments) {
			try {
				final byte[] attachmentBytes = this.contentHostingService.getResource(attachment.getAttachmentId()).getContent();
				this.archiverService.archiveContent(archiveId, siteId, toolId, attachmentBytes,
						attachment.getAttachmentName(), subdir);
			} catch (ServerOverloadException | IdUnusedException | TypeException | PermissionException e) {
				log.error("Error getting attachment with ID: ", attachment.getId());
				continue;
			}
		}
	}

	/**
	 * From ForumEntityProviderImpl
	 *
	 * This is a dirty hack to set the attachments on the message. There doesn't seem to be an api for getting a single message with all
	 * attachments. If you try and retrieve them after, hibernate throws a lazy exception.
	 *
	 * @param unPopulatedMessage The message we want to set attachments on
	 * @param populatedMessages The list of populated messages retrieved from the forum manager
	 */
	private void setAttachments(final Message unPopulatedMessage, final List<Message> populatedMessages) {

		for (final Message populatedMessage : populatedMessages) {
			if (populatedMessage.getId().equals(unPopulatedMessage.getId())
					&& populatedMessage.getHasAttachments()) {
				unPopulatedMessage.setAttachments(populatedMessage.getAttachments());
				break;
			}
		}
	}

	/**
	 * Simplified Message class
	 */
	private class SimpleMessage {

		@Setter
		@Getter
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

		@Setter
		@Getter
		private List<SimpleMessage> replies = new ArrayList<SimpleMessage>();

		public SimpleMessage(final Message message) {

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

			final Message parent = message.getInReplyTo();
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

		public SimpleTopic(final DiscussionTopic topic) {
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

		@Setter
		private List<SimpleTopic> topics;

		public SimpleForum(final DiscussionForum forum) {
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
