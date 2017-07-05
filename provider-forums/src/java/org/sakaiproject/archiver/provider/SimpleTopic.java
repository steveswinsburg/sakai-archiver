package org.sakaiproject.archiver.provider;

import org.sakaiproject.api.app.messageforums.DiscussionTopic;
import org.sakaiproject.archiver.util.Dateifier;

import lombok.Setter;

/**
 * Simplified Topic class
 */

public class SimpleTopic extends SimpleArchiveItem {

	@Setter
	private Long topicId;

	@Setter
	private String title;

	@Setter
	private String createdDate;

	@Setter
	private String creator;

	public SimpleTopic(final DiscussionTopic topic) {
		this.topicId = topic.getId();
		this.title = topic.getTitle();
		this.createdDate = Dateifier.toIso8601(topic.getCreated());
		this.creator = topic.getCreatedBy();
	}
}
