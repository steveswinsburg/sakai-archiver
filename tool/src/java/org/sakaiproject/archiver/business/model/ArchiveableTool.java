package org.sakaiproject.archiver.business.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Wrapper for the set of tools that are configured as archiveable.
 *
 */
public class ArchiveableTool {

	@Getter
	private final String toolId;

	@Getter
	@Setter
	private String name;

	@Getter
	@Setter
	private boolean includeInArchive;

	public ArchiveableTool(final String toolId, final String name) {
		this.toolId = toolId;
		this.name = name;
	}
}
