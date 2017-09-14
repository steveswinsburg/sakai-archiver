package org.sakaiproject.archiver.provider;

import java.util.ArrayList;
import java.util.List;

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

	@Setter
	@Getter
	private List<String> conversationLinks = new ArrayList<>();

	@Setter
	private String modifiedDate;

	@Setter
	private String modifier;

	@Setter
	private Boolean isLocked;

	@Setter
	private Boolean isPostFirst;

	@Setter
	private String assocGradebookItemName;

	@Setter
	private String openDate;

	@Setter
	private String closeDate;

	public SimpleTopic(final DiscussionTopic topic) {
		this.title = topic.getTitle();
		this.shortDescription = topic.getShortDescription();
		this.extendedDescription = topic.getExtendedDescription();
	}
}
