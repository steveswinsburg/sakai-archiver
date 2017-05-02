package org.sakaiproject.archiver.app.model;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import lombok.Getter;
import lombok.Setter;

/**
 * Backs the create archive form
 */
public class ArchiveSettings implements Serializable {

	private static final long serialVersionUID = 1L;

	@Getter
	@Setter
	private boolean includeStudentData;

	@Getter
	@Setter
	private List<ArchiveableTool> archiveableTools;

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}

}
