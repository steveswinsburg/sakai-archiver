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
		this.topicId = topic.getId();
		this.title = topic.getTitle();
		this.createdDate = Dateifier.toIso8601(topic.getCreated());
		this.creator = topic.getCreatedBy();
		this.modifiedDate = Dateifier.toIso8601(topic.getModified());
		this.modifier = topic.getModifiedBy();
		this.isLocked = topic.getLocked();
		this.isPostFirst = topic.getPostFirst();
		this.assocGradebookItemName = topic.getDefaultAssignName();

		// if availability is restricted, get open and close dates
		if (topic.getAvailabilityRestricted()) {
			this.openDate = Dateifier.toIso8601(topic.getOpenDate());
			this.closeDate = Dateifier.toIso8601(topic.getCloseDate());
		}
	}
}
