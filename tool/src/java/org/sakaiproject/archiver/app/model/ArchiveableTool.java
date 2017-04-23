package org.sakaiproject.archiver.app.model;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import lombok.Getter;
import lombok.Setter;

/**
 * Wrapper for the set of tools that are configured as archiveable.
 *
 */
public class ArchiveableTool implements Serializable {

	private static final long serialVersionUID = 1L;

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
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}

}
