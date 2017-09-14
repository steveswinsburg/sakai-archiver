package org.sakaiproject.archiver.provider;

import java.util.List;

import org.sakaiproject.api.app.messageforums.DiscussionForum;

import lombok.Getter;
import lombok.Setter;

/**
 * Simplified Forum task
 */
public class SimpleForum extends SimpleArchiveItem {

	@Setter
	@Getter
	private String title;

	@Setter
	@Getter
	private String extendedDescription;

	@Setter
	@Getter
	private String shortDescription;

	@Setter
	@Getter
	private List<SimpleTopic> topics;

	public SimpleForum(final DiscussionForum forum) {
		this.title = forum.getTitle();
		this.extendedDescription = forum.getExtendedDescription();
		this.shortDescription = forum.getShortDescription();
	}
}
