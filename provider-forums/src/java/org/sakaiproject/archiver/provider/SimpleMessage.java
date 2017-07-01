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
	private String title;

	@Setter
	private String body;

	@Setter
	private String lastModified;

	@Setter
	private String authoredBy;

	@Setter
	private String authorId;

	@Setter
	private Long replyTo;

	@Setter
	private String createdOn;

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
		this.lastModified = Dateifier.toIso8601(message.getModified());
		this.authoredBy = message.getAuthor();
		this.authorId = message.getAuthorId();
		this.isDraft = message.getDraft();
		this.isDeleted = message.getDeleted();
		this.createdOn = Dateifier.toIso8601(message.getCreated());
		this.modifiedBy = message.getModifiedBy();

		final Message parent = message.getInReplyTo();
		if (parent != null) {
			this.replyTo = parent.getId();
		}
	}
}
