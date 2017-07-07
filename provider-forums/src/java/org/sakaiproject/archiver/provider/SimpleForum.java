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
	private String createdDate;

	@Setter
	private String createdBy;

	@Setter
	private List<SimpleTopic> topics;

	public SimpleForum(final DiscussionForum forum) {
		this.title = forum.getTitle();
		this.extendedDescription = forum.getExtendedDescription();
		this.shortDescription = forum.getShortDescription();
		this.createdDate = Dateifier.toIso8601(forum.getCreated());
		this.createdBy = forum.getCreatedBy();
	}
}
