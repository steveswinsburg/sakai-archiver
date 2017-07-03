package org.sakaiproject.archiver.provider;

import java.util.List;

import org.sakaiproject.api.app.messageforums.DiscussionForum;
import org.sakaiproject.archiver.util.Dateifier;

import lombok.Setter;

/**
 * Simplified Forum task
 */
public class SimpleForum extends SimpleArchiveItem {

	@Setter
	private String title;

	@Setter
	private String extendedDescription;

	@Setter
	private String shortDescription;

	@Setter
	private Boolean isLocked;

	@Setter
	private Boolean isModerated;

	@Setter
	private Boolean isPostFirst;

	@Setter
	private String createdDate;

	@Setter
	private String createdBy;

	@Setter
	private String openDate;

	@Setter
	private String closeDate;

	@Setter
	private String assocGradebookItemName;

	@Setter
	private Boolean isDraft;

	@Setter
	private String modifiedDate;

	@Setter
	private String modifiedBy;

	@Setter
	private List<SimpleTopic> topics;

	public SimpleForum(final DiscussionForum forum) {
		this.title = forum.getTitle();
		this.extendedDescription = forum.getExtendedDescription();
		this.shortDescription = forum.getShortDescription();
		this.isLocked = forum.getLocked();
		this.isModerated = forum.getModerated();
		this.isPostFirst = forum.getPostFirst();
		this.createdDate = Dateifier.toIso8601(forum.getCreated());
		this.createdBy = forum.getCreatedBy();
		this.assocGradebookItemName = forum.getDefaultAssignName();
		this.isDraft = forum.getDraft();
		this.modifiedDate = Dateifier.toIso8601(forum.getModified());
		this.modifiedBy = forum.getModifiedBy();

		// if availability is restricted, get open and close dates
		if (forum.getAvailabilityRestricted()) {
			this.openDate = Dateifier.toIso8601(forum.getOpenDate());
			this.closeDate = Dateifier.toIso8601(forum.getCloseDate());
		}
	}
}
