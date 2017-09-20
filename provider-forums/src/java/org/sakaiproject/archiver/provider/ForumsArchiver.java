package org.sakaiproject.archiver.provider;

import java.util.ArrayList;
import java.util.Collections;
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
import org.sakaiproject.archiver.util.Sanitiser;
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

	private String toolName;

	@SuppressWarnings("unchecked")
	@Override
	public void archive(final String archiveId, final String siteId, final boolean includeStudentContent) {

		final List<DiscussionForum> forums = this.forumManager.getDiscussionForumsWithTopics(siteId);

		this.toolName = getToolName(siteId);

		for (final DiscussionForum forum : forums) {
			// Archive the forum, including topics (and messages if includeStudentContent is true)
			archiveForum(forum, archiveId, siteId, includeStudentContent);
		}
	}

	@Override
	public String getToolName(final String siteId) {
		return this.archiverService.getToolName(siteId, TOOL_ID);
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
		List<SimpleTopic> simpleTopics = new ArrayList<>();

		final List<DiscussionTopic> topics = forum.getTopics();

		// Archive the topics and messages
		for (final DiscussionTopic topic : topics) {
			simpleTopics = archiveTopics(forum, topic, simpleTopics, archiveId, siteId, includeStudentContent);
		}

		// Archive the attachments for this forum
		final List<String> forumAttachmentsLoc = new ArrayList<>();
		forumAttachmentsLoc.add(Sanitiser.sanitise(forum.getTitle()));
		forumAttachmentsLoc.add("forum-attachments");
		archiveAttachments(forum.getAttachments(), forumAttachmentsLoc, archiveId, siteId, simpleForum);
		finaliseAttachmentsHtml(simpleForum);

		// Now that all the topics are set, archive the forum
		simpleForum.setTopics(simpleTopics);
		final String forumHtml = getForumHtml(simpleForum);
		final String finalForumHtml = Htmlifier.toHtml(forumHtml, this.archiverService.getSiteHeader(siteId, TOOL_ID));
		this.archiverService.archiveContent(archiveId, siteId, this.toolName, finalForumHtml.getBytes(), forum.getTitle() + ".html",
				forum.getTitle());
	}

	/**
	 * Archive the topics for this forum, and their messages
	 *
	 * @param forum
	 * @param topic
	 * @param simpleTopics
	 * @param archiveId
	 * @param siteId
	 * @param includeStudentContent
	 * @return the list of topics
	 */
	private List<SimpleTopic> archiveTopics(final DiscussionForum forum, final DiscussionTopic topic, final List<SimpleTopic> simpleTopics,
			final String archiveId, final String siteId, final boolean includeStudentContent) {

		final SimpleTopic simpleTopic = new SimpleTopic(topic);

		// Add this topic to the forum's topic array
		simpleTopics.add(simpleTopic);

		// Set up the folder structure for saving attachments and messages
		final List<String> folderStructure = new ArrayList<>();
		folderStructure.add(Sanitiser.sanitise(forum.getTitle()));
		folderStructure.add("topics");
		folderStructure.add(Sanitiser.sanitise(topic.getTitle()));

		// Set up the folder structure for saving topic attachments
		final List<String> topicAttachmentsLoc = new ArrayList<>();
		topicAttachmentsLoc.addAll(folderStructure);
		topicAttachmentsLoc.add("topic-attachments");

		// Archive the attachments for this topic
		archiveAttachments(topic.getAttachments(), topicAttachmentsLoc, archiveId, siteId, simpleTopic);
		finaliseAttachmentsHtml(simpleTopic);

		// Archive the messages within a topic, if we want student content
		if (includeStudentContent) {

			// Sort the messages so that the oldest are at the top
			final List<Message> messages = topic.getMessages();
			Collections.sort(messages, new DateComparator());

			// Archive the messages for this topic
			archiveMessages(messages, topic, simpleTopic, folderStructure, archiveId, siteId);
		}
		return simpleTopics;
	}

	/**
	 * Archive all the messages associated with this topic
	 *
	 * @param messages
	 * @param topic
	 * @param simpleTopic
	 * @param folderStructure
	 * @param archiveId
	 * @param siteId
	 */
	private void archiveMessages(final List<Message> messages, final DiscussionTopic topic, final SimpleTopic simpleTopic,
			final List<String> folderStructure, final String archiveId, final String siteId) {

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
				final String conversationHtml = getMessageHtml(topLevelMessage);
				final String finalConversationHtml = Htmlifier.toHtml(conversationHtml,
						this.archiverService.getSiteHeader(siteId, TOOL_ID));
				this.archiverService.archiveContent(archiveId, siteId, this.toolName, finalConversationHtml.getBytes(),
						message.getTitle() + ".html", folderStructure.toArray(new String[folderStructure.size()]));

				// Add a link of the conversation location to the SimpleTopic associated with this conversation
				final List<String> linksToTopicConversations = simpleTopic.getConversationLinks();
				linksToTopicConversations.add("<a href=\"./topics/" + Sanitiser.sanitise(simpleTopic.getTitle()) + "/"
						+ Sanitiser.sanitise(message.getTitle()) + ".html"
						+ "\">" + message.getTitle() + ".html" + "</a> ");
				simpleTopic.setConversationLinks(linksToTopicConversations);
			}
		}
	}

	/**
	 * Get the html string for the forum
	 *
	 * @param simpleForum
	 * @return forumHtml
	 */
	private String getForumHtml(final SimpleForum simpleForum) {

		final StringBuilder sb = new StringBuilder();

		sb.append("<h2>Forum: " + simpleForum.getTitle() + "</h2>");
		sb.append("<p>Short Description: " + simpleForum.getShortDescription() + "</p>");
		sb.append("<p>Full Description: " + simpleForum.getExtendedDescription() + "</p>");
		sb.append("<p>" + simpleForum.getAttachments() + "</p>");

		for (final SimpleTopic topic : simpleForum.getTopics()) {
			sb.append("<h3>Topic: " + topic.getTitle() + "</h3>");
			sb.append("<p>Short Description: " + topic.getShortDescription() + "</p>");
			sb.append("<p>Full Description: " + topic.getExtendedDescription() + "</p>");
			sb.append("<p>" + topic.getAttachments() + "</p>");
			if (!topic.getConversationLinks().isEmpty()) {
				sb.append("<p>Conversations: ");
				for (final String loc : topic.getConversationLinks()) {
					sb.append("<p>" + loc + "</p>");
				}
				sb.append("</p>");
			}
		}

		return sb.toString();
	}

	/**
	 * Get the html string for a conversation
	 *
	 * @param msg
	 * @return conversationHtml
	 */
	private String getMessageHtml(final SimpleMessage msg) {
		final StringBuilder sb = new StringBuilder();

		sb.append("<h4>" + msg.getTitle() + "</h4>");

		sb.append(String.format("<p>%s (%s)</p>", msg.getAuthoredBy(), msg.getCreatedOn()));
		sb.append("<p>" + msg.getBody() + "</p>");
		sb.append("<p>" + msg.getAttachments() + "</p>");

		if (!msg.getReplies().isEmpty()) {
			sb.append(getRepliesHtml(msg));
		}

		return sb.toString();
	}

	/**
	 * Get the html string for all the replies for a message
	 *
	 * @param msg
	 * @return repliesHtml
	 */
	private String getRepliesHtml(final SimpleMessage msg) {

		final StringBuilder sb = new StringBuilder();

		for (final SimpleMessage reply : msg.getReplies()) {
			sb.append("<blockquote>");
			sb.append(String.format("<p>%s (%s)</p>", reply.getAuthoredBy(), reply.getCreatedOn()));
			sb.append("<p>" + reply.getBody() + "</p>");
			sb.append("<p>" + reply.getAttachments() + "</p>");

			if (!reply.getReplies().isEmpty()) {
				sb.append(getRepliesHtml(reply));
			}
			sb.append("</blockquote>");
		}

		return sb.toString();
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
	private void setMessageReplies(final SimpleMessage simpleTopMessage, final List<Message> messages, final List<String> folderStructure,
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
	private void addAttachmentsToMessage(final Message message, final SimpleMessage simpleMessage, final List<String> folderStructure,
			final String archiveId, final String siteId, final Long topicId) {
		final Topic topicWithMessageAttachments = this.forumManager.getTopicByIdWithMessagesAndAttachments(topicId);
		setAttachments(message, topicWithMessageAttachments.getMessages());

		final List<String> messageAttachmentsLoc = new ArrayList<>();
		messageAttachmentsLoc.addAll(folderStructure);
		messageAttachmentsLoc.add("message-attachments");
		messageAttachmentsLoc.add("message-" + message.getId());
		archiveAttachments(message.getAttachments(), messageAttachmentsLoc, archiveId, siteId, simpleMessage);
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
	private void archiveAttachments(final List<Attachment> attachments, final List<String> subdirs,
			final String archiveId, final String siteId, final SimpleArchiveItem simpleArchiveItem) {
		for (final Attachment attachment : attachments) {
			try {
				final byte[] attachmentBytes = this.contentHostingService.getResource(attachment.getAttachmentId()).getContent();
				this.archiverService.archiveContent(archiveId, siteId, this.toolName, attachmentBytes,
						attachment.getAttachmentName(), subdirs.toArray(new String[subdirs.size()]));
				// update the attachments HTML string, so there is a link to this attachment in the html file
				addToAttachmentsHtml(subdirs, attachment.getAttachmentName(), simpleArchiveItem);
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
	private void addToAttachmentsHtml(final List<String> attachmentLocation, final String attachmentName,
			final SimpleArchiveItem simpleArchiveItem) {

		String attachmentHyperlink;

		String fullDir = "";

		for (final String folder : attachmentLocation) {
			fullDir += folder + "/";
		}
		fullDir += Sanitiser.sanitise(attachmentName);

		if (simpleArchiveItem instanceof SimpleMessage) {
			attachmentHyperlink = "<li><a href=\"../../../" + fullDir + "\">" + attachmentName
					+ "</a></li>";
		} else {
			attachmentHyperlink = "<li><a href=\"../" + fullDir + "\">" + attachmentName
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
