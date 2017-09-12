package org.sakaiproject.archiver.provider;

import org.sakaiproject.api.app.messageforums.DiscussionTopic;

import lombok.Getter;
import lombok.Setter;

/**
 * Simplified Topic class
 */

public class SimpleTopic extends SimpleArchiveItem {

	@Setter
	@Getter
	private String title;

	@Setter
	@Getter
	private String shortDescription;

	@Setter
	@Getter
	private String extendedDescription;

	public SimpleTopic(final DiscussionTopic topic) {
		this.title = topic.getTitle();
		this.shortDescription = topic.getShortDescription();
		this.extendedDescription = topic.getExtendedDescription();
	}
}
