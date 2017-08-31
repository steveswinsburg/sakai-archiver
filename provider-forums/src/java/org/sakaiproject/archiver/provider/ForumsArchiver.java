package org.sakaiproject.archiver.provider;

import java.util.ArrayList;
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
import org.sakaiproject.archiver.util.Htmlifier;
import org.sakaiproject.content.api.ContentHostingService;
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
	private static final String TOOL_NAME = "Forums";

	public void init() {
		ArchiverRegistry.getInstance().register(TOOL_ID, this);
	}

	public void destroy() {
		ArchiverRegistry.getInstance().unregister(TOOL_ID);
	}

	@Setter
	private DiscussionForumManager forumManager;

	@Setter
	private ContentHostingService contentHostingService;

	@Setter
	private ArchiverService archiverService;

	@SuppressWarnings("unchecked")
	@Override
	public void archive(final String archiveId, final String siteId, final boolean includeStudentContent) {

		final List<DiscussionForum> forums = this.forumManager.getDiscussionForumsWithTopics(siteId);

		for (final DiscussionForum forum : forums) {
			// Archive the forum, including topics (and messages if includeStudentContent is true)
			archiveForum(forum, archiveId, siteId, includeStudentContent);

		}
	}

	/**
	 * Archive the forum and its topics, messages and attachments
	 *
	 * @param forum
	 * @param archiveId
	 * @param siteId
	 */
	@SuppressWarnings("unchecked")
	private void archiveForum(final DiscussionForum forum, final String archiveId, final String siteId,
			final boolean includeStudentContent) {

		// Set up the simple forum object
		final SimpleForum simpleForum = new SimpleForum(forum);

		// Initialise the array to hold each topic for this forum
		final List<SimpleTopic> simpleTopics = new ArrayList<>();

		final List<DiscussionTopic> topics = forum.getTopics();
		for (final DiscussionTopic topic : topics) {
			final SimpleTopic simpleTopic = new SimpleTopic(topic);

			// Add this topic to the forum's topic array
			simpleTopics.add(simpleTopic);

			// Set up the folder structure for saving attachments and messages
			final String folderStructure = forum.getTitle() + "/topics/" + topic.getTitle();

			// Archive the attachments for this topic
			archiveAttachments(topic.getAttachments(), folderStructure + "/topic-attachments/", archiveId, siteId, simpleTopic);
			finaliseAttachmentsHtml(simpleTopic);

			// Archive the messages within a topic, if we want student content
			if (includeStudentContent) {

				final List<Message> messages = topic.getMessages();
				for (final Message message : messages) {

					// Find the top level message
					if (message.getInReplyTo() == null) {
						final SimpleMessage topLevelMessage = new SimpleMessage(message);

						// Set message replies and archive the attachments for each message
						setMessageReplies(topLevelMessage, messages, folderStructure, archiveId, siteId, topic.getId());

						// Archive the attachments for the top level message
						if (message.getHasAttachments()) {
							addAttachmentsToMessage(message, topLevelMessage, folderStructure, archiveId, siteId, topic.getId());
							finaliseAttachmentsHtml(topLevelMessage);
						}

						// Archive the messages
						final String messageHtml = Htmlifier.addSiteHeader(Htmlifier.toHtml(topLevelMessage),
								this.archiverService.getSiteHeader(siteId, TOOL_ID));
						this.archiverService.archiveContent(archiveId, siteId, TOOL_NAME, messageHtml.getBytes(),
								message.getTitle() + ".html", folderStructure);
					}
				}
			}
		}

		// Archive the attachments for this forum
		archiveAttachments(forum.getAttachments(), forum.getTitle() + "/forum-attachments/", archiveId,
				siteId, simpleForum);
		finaliseAttachmentsHtml(simpleForum);

		// Now that all the topics are set, archive the forum
		simpleForum.setTopics(simpleTopics);
		final String forumHtml = Htmlifier.addSiteHeader(Htmlifier.toHtml(simpleForum),
				this.archiverService.getSiteHeader(siteId, TOOL_ID));
		this.archiverService.archiveContent(archiveId, siteId, TOOL_NAME, forumHtml.getBytes(), forum.getTitle() + ".html",
				forum.getTitle());
	}

	/**
	 * Add any replies to each SimpleMessage object
	 *
	 * @param simpleTopMessage The first message posted in a topic, as a SimpleMessage
	 * @param messages The full list of messages for this topic
	 * @param siteId
	 * @param archiveId
	 * @param folderStructure
	 */
	private void setMessageReplies(final SimpleMessage simpleTopMessage, final List<Message> messages, final String folderStructure,
			final String archiveId, final String siteId, final Long topicId) {

		for (final Message message : messages) {
			// if this message is in reply to the top message
			if ((message.getInReplyTo() != null) && (message.getInReplyTo().getId() == simpleTopMessage.getMessageId())) {
				final List<SimpleMessage> replies = simpleTopMessage.getReplies();
				final SimpleMessage thisMessage = new SimpleMessage(message);
				replies.add(thisMessage);

				// Archive the attachments for this message
				// This has to be done here since we need to set the attachments html string for each message as the attachments are saved
				if (message.getHasAttachments()) {
					addAttachmentsToMessage(message, thisMessage, folderStructure, archiveId, siteId, topicId);
				}
				// Recursively set the replies for this inner message
				setMessageReplies(thisMessage, messages, folderStructure, archiveId, siteId, topicId);

			}
		}
	}

	/**
	 * Add the attachments to the passed in message, since they are not already attached. See doc on setAttachments().
	 *
	 * @param message
	 * @param simpleMessage
	 * @param folderStructure
	 * @param archiveId
	 * @param siteId
	 * @param topicId
	 */
	@SuppressWarnings("unchecked")
	private void addAttachmentsToMessage(final Message message, final SimpleMessage simpleMessage, final String folderStructure,
			final String archiveId, final String siteId, final Long topicId) {
		final Topic topicWithMessageAttachments = this.forumManager.getTopicByIdWithMessagesAndAttachments(topicId);
		setAttachments(message, topicWithMessageAttachments.getMessages());

		archiveAttachments(message.getAttachments(),
				folderStructure + "/topic-attachments/message-attachments/message-" + message.getId() + "/", archiveId, siteId,
				simpleMessage);
		finaliseAttachmentsHtml(simpleMessage);
	}

	/**
	 * Archive a list of attachments
	 *
	 * @param assignment
	 * @param archiveId
	 * @param siteId
	 * @param simpleArchiveItem the object that the attachmentHtml needs to be updated for (SimpleMessage, SimpleTopic or SimpleForum)
	 */
	private void archiveAttachments(final List<Attachment> attachments, final String subdir,
			final String archiveId, final String siteId, final SimpleArchiveItem simpleArchiveItem) {
		for (final Attachment attachment : attachments) {
			try {
				final byte[] attachmentBytes = this.contentHostingService.getResource(attachment.getAttachmentId()).getContent();
				this.archiverService.archiveContent(archiveId, siteId, TOOL_NAME, attachmentBytes,
						attachment.getAttachmentName(), subdir);
				// update the attachments HTML string, so there is a link to this attachment in the html file
				addToAttachmentsHtml(subdir, attachment.getAttachmentName(), simpleArchiveItem);
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
	 * Set the html string that contains a list of attachment hyperlinks
	 *
	 * @param attachmentLocation
	 * @param attachmentName
	 * @param simpleArchiveItem
	 */
	private void addToAttachmentsHtml(final String attachmentLocation, final String attachmentName,
			final SimpleArchiveItem simpleArchiveItem) {

		String attachmentHyperlink;
		if (simpleArchiveItem instanceof SimpleMessage) {
			attachmentHyperlink = "<li><a href=\"../../../" + attachmentLocation + attachmentName + "\">" + attachmentName
					+ "</a></li>";
		} else {
			attachmentHyperlink = "<li><a href=\"../" + attachmentLocation + attachmentName + "\">" + attachmentName
					+ "</a></li>";
		}

		simpleArchiveItem.setAttachments(simpleArchiveItem.getAttachments() + attachmentHyperlink);
	}

	/**
	 * Finalise the attachments html string by surrounding it by unordered list tags
	 *
	 * @param simpleArchiveItem
	 */
	private void finaliseAttachmentsHtml(final SimpleArchiveItem simpleArchiveItem) {

		simpleArchiveItem.setAttachments("<ul style=\"list-style: none;padding-left:0;\">" + simpleArchiveItem.getAttachments() + "</ul>");

	}
}
