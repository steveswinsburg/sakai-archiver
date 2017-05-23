package org.sakaiproject.archiver.provider;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.sakaiproject.archiver.api.Archiveable;
import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.util.Jsonifier;
import org.sakaiproject.chat2.model.ChatChannel;
import org.sakaiproject.chat2.model.ChatManager;
import org.sakaiproject.chat2.model.ChatMessage;
import org.sakaiproject.exception.PermissionException;
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

	private static final String CHAT_TOOL = "sakai.chat";

	public void init() {
		ArchiverRegistry.getInstance().register(CHAT_TOOL, this);
	}

	public void destroy() {
		ArchiverRegistry.getInstance().unregister(CHAT_TOOL);
	}

	@Setter
	private ChatManager chatManager;

	@Setter
	private ArchiverService archiverService;

	@Setter
	private UserDirectoryService userDirectoryService;

	@Override
	public void archive(final String archiveId, final String siteId, final String toolId, final boolean includeStudentContent) {

		List<ChatChannel> chatChannels = chatManager.getContextChannels(siteId, true);

		for (ChatChannel chatChannel : chatChannels) {

			// Save the metadata for this channel
			ChatChannelMetadata metadata = new ChatChannelMetadata();
			metadata.setDateCreated(chatChannel.getCreationDate());
			metadata.setStartDate(chatChannel.getStartDate());
			metadata.setEndDate(chatChannel.getEndDate());
			metadata.setTitle(chatChannel.getTitle());
			metadata.setDescription(chatChannel.getDescription());
			metadata.setId(chatChannel.getId());
			this.archiverService.archiveContent(archiveId, siteId, toolId, Jsonifier.toJson(metadata).getBytes(), 
					chatChannel.getTitle() + " (metadata).json");

			int numMessages = chatManager.getChannelMessagesCount(chatChannel, null, null);

			// Go through and get chat messages, 99 at a time (i.e. 0-99, 100-199, etc)
			for (int start = 0; start <= numMessages - (numMessages%100); start += 100) {

				try {
					List<ChatMessage> chatMessages = chatManager.getChannelMessages(chatChannel, null, null, start, 99, true);

					List<SimpleChatMessage> messagesToSave = new ArrayList<SimpleChatMessage>();
					for (ChatMessage message : chatMessages) {
						SimpleChatMessage simpleChatMessage = new SimpleChatMessage();
						simpleChatMessage.setBody(message.getBody());
						simpleChatMessage.setDate(message.getMessageDate());
						try {
							simpleChatMessage.setOwner(userDirectoryService.getUser(message.getOwner()).getDisplayName());
							simpleChatMessage.setUserId(userDirectoryService.getUser(message.getOwner()).getEid());

						} catch (UserNotDefinedException e) {
							log.error("Could not find user with userId " + message.getOwner());
							continue;
						}
						messagesToSave.add(simpleChatMessage);
					}

					// Convert to JSON and save to file
					int rangeStart = start+1;
					int rangeEnd = numMessages - start >= 100 ? start+100 : numMessages+1;
					this.archiverService.archiveContent(archiveId, siteId, toolId, Jsonifier.toJson(messagesToSave).getBytes(), 
							chatChannel.getTitle() + "(" + rangeStart + "-" + rangeEnd + ").json", "Chat messages for " + chatChannel.getTitle());

				} catch (PermissionException e) {
					log.error("Could not retrieve some chat messages for channel: " + chatChannel.getTitle());
					continue;
				}
			}
		}
	}

	/**
	 * Simplified helper class to represent an individual chat message
	 */
	private static class SimpleChatMessage {

		@Getter @Setter
		private String body;

		@Getter @Setter
		private Date date;

		@Getter @Setter
		private String owner;

		@Getter @Setter
		private String userId;

	}

	/**
	 * Simplified helper class to represent the metadata for a chat channel
	 */
	private static class ChatChannelMetadata {

		@Getter @Setter
		private String title;

		@Getter @Setter
		private String description;

		@Getter @Setter
		private String id;

		@Getter @Setter
		private Date dateCreated;

		@Getter @Setter
		private Date startDate;

		@Getter @Setter
		private Date endDate;
	}
}
