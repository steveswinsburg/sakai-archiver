package org.sakaiproject.archiver.provider;

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

	@Override
	public void archive(final String archiveId, final String siteId, final String toolId, final boolean includeStudentContent) {

		List<ChatChannel> chatChannels = chatManager.getContextChannels(siteId, true);
		
		for (ChatChannel chatChannel : chatChannels) {
			int numMessages = chatManager.getChannelMessagesCount(chatChannel, null, null);
			
			// Go through and get chat messages, 99 at a time (i.e. 0-99, 100-199, etc)
			for (int start = 0; start <= numMessages - (numMessages%100); start += 100) {
				
				try {
					List<ChatMessage> chatMessages = chatManager.getChannelMessages(chatChannel, null, null, start, 99, true);
					
					// Convert to JSON and save to file
					String jsonArchiveItem = Jsonifier.toJson(chatMessages, "yyyy-MM-dd HH:mm:ss.SSS");
					log.debug("Chat messages archive item: " + jsonArchiveItem);
					
					int endRange = numMessages - start >= 100 ? start+99 : numMessages;
					this.archiverService.archiveContent(archiveId, siteId, CHAT_TOOL, jsonArchiveItem.getBytes(), 
							chatChannel.getTitle() + "(" + start + "-" + endRange + ").json");
					
				} catch (PermissionException e) {
					log.error("Could not retrieve some chat messages for channel: " + chatChannel.getTitle());
					continue;
				}
			}
		}
	}
}
