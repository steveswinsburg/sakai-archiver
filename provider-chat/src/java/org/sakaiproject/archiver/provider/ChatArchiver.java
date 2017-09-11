package org.sakaiproject.archiver.provider;

import java.util.ArrayList;
import java.util.List;

import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.spi.Archiveable;
import org.sakaiproject.archiver.util.Dateifier;
import org.sakaiproject.archiver.util.Htmlifier;
import org.sakaiproject.chat2.model.ChatChannel;
import org.sakaiproject.chat2.model.ChatManager;
import org.sakaiproject.chat2.model.ChatMessage;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link Archiveable} for the chat tool
 */
@Slf4j
public class ChatArchiver implements Archiveable {

	private static final String TOOL_ID = "sakai.chat";
	private static final String TOOL_NAME = "Chat";

	public void init() {
		ArchiverRegistry.getInstance().register(TOOL_ID, this);
	}

	public void destroy() {
		ArchiverRegistry.getInstance().unregister(TOOL_ID);
	}

	@Setter
	private ChatManager chatManager;

	@Setter
	private ArchiverService archiverService;

	@Setter
	private UserDirectoryService userDirectoryService;

	@Override
	public void archive(final String archiveId, final String siteId, final boolean includeStudentContent) {

		if (!includeStudentContent) {
			log.warn("Chat data cannot be archived without student content enabled");
			return;
		}

		final List<ChatChannel> chatChannels = this.chatManager.getContextChannels(siteId, true);

		for (final ChatChannel chatChannel : chatChannels) {

			final int numMessages = this.chatManager.getChannelMessagesCount(chatChannel, null, null);

			// Go through and get chat messages, 99 at a time (i.e. 0-99, 100-199, etc)
			for (int start = 0; start <= numMessages - (numMessages % 100); start += 100) {

				try {
					final List<ChatMessage> chatMessages = this.chatManager.getChannelMessages(chatChannel, null, null, start, 99,
							true);

					final List<SimpleChatMessage> messagesToSave = createArchiveItems(chatMessages);

					// Convert to HTML and save to file
					final int rangeStart = start + 1;
					final int rangeEnd = numMessages - start >= 100 ? start + 100 : numMessages;

					final String chatHtml = getAsHtml(messagesToSave);
					final String finalChatHtml = Htmlifier.toHtml(chatHtml, this.archiverService.getSiteHeader(siteId, TOOL_ID));
					log.debug("Chat HTML: " + finalChatHtml);

					this.archiverService.archiveContent(archiveId, siteId, TOOL_NAME, finalChatHtml.getBytes(),
							chatChannel.getTitle() + " (" + rangeStart + "-" + rangeEnd + ").html",
							chatChannel.getTitle());

				} catch (final PermissionException e) {
					log.error("Could not retrieve some chat messages for channel: " + chatChannel.getTitle(), e);
					continue;
				}
			}

		}
	}

	private String getAsHtml(final List<SimpleChatMessage> msgs) {
		final StringBuilder sb = new StringBuilder();

		for (final SimpleChatMessage msg : msgs) {
			sb.append("<p>" + msg.getOwner() + "&nbsp;(" + msg.getDate() + "): " + msg.getBody() + "</p>");
		}

		return sb.toString();
	}

	/**
	 * Build the list of messages to be archived for this channel
	 *
	 * @param chatMessages
	 * @return the list of messages to be saved
	 */
	private List<SimpleChatMessage> createArchiveItems(final List<ChatMessage> chatMessages) {
		final List<SimpleChatMessage> messagesToSave = new ArrayList<>();
		for (final ChatMessage message : chatMessages) {
			final SimpleChatMessage simpleChatMessage = createArchiveItem(message);
			messagesToSave.add(simpleChatMessage);
		}

		return messagesToSave;
	}

	/**
	 * Build the archive item for an individual chat message
	 *
	 * @param message
	 * @return
	 */

	private SimpleChatMessage createArchiveItem(final ChatMessage message) {

		final SimpleChatMessage simpleChatMessage = new SimpleChatMessage();
		simpleChatMessage.setBody(message.getBody());
		simpleChatMessage.setDate(Dateifier.toIso8601(message.getMessageDate()));

		final User user = getUser(message.getOwner());
		simpleChatMessage.setOwner((user != null) ? user.getDisplayName() : message.getOwner());

		return simpleChatMessage;
	}

	/**
	 * Helper to get the user associated with a chat message
	 *
	 * @param owner
	 * @return user
	 */
	private User getUser(final String owner) {

		User user;

		try {
			user = this.userDirectoryService.getUser(owner);
			return user;

		} catch (final UserNotDefinedException e) {
			log.warn("Could not find user with userId: {}. Uuid will be used in place of display name and eid.", owner);
		}
		return null;
	}

	/**
	 * Simplified helper class to represent an individual chat message
	 */
	private class SimpleChatMessage {

		@Getter
		@Setter
		private String body;

		@Getter
		@Setter
		private String date;

		@Getter
		@Setter
		private String owner;
	}
}
