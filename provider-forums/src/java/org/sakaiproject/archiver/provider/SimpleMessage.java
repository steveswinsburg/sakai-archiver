package org.sakaiproject.archiver.provider;

import java.util.ArrayList;
import java.util.List;

import org.sakaiproject.api.app.messageforums.Message;
import org.sakaiproject.archiver.util.Dateifier;

import lombok.Getter;
import lombok.Setter;

/**
 * Simplified Message class
 */
public class SimpleMessage extends SimpleArchiveItem {
	@Setter
	@Getter
	private Long messageId;

	@Setter
	@Getter
	private String title;

	@Setter
	@Getter
	private String body;

	@Setter
	@Getter
	private String authoredBy;

	@Setter
	private Long replyTo;

	@Setter
	@Getter
	private String createdOn;

	@Setter
	@Getter
	private List<SimpleMessage> replies = new ArrayList<>();

	public SimpleMessage(final Message message) {

		this.messageId = message.getId();
		this.title = message.getTitle();
		this.body = message.getBody();
		this.authoredBy = message.getAuthor();
		this.createdOn = Dateifier.toIso8601(message.getCreated());

		final Message parent = message.getInReplyTo();
		if (parent != null) {
			this.replyTo = parent.getId();
		}
	}
}
