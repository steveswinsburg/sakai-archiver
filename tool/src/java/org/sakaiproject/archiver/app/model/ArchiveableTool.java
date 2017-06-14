package org.sakaiproject.archiver.app.model;

import java.io.Serializable;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import lombok.Getter;
import lombok.Setter;

/**
 * Wrapper for the set of tools that are configured as archiveable.
 *
 */
public class ArchiveableTool implements Serializable, Comparable<ArchiveableTool> {

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

	@Override
	public int compareTo(final ArchiveableTool o) {
		return new CompareToBuilder()
				.append(this.name, o.name)
				.append(this.toolId, o.toolId)
				.toComparison();
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null) {
			return false;
		}
		if (o == this) {
			return true;
		}
		if (o.getClass() != getClass()) {
			return false;
		}
		final ArchiveableTool other = (ArchiveableTool) o;
		return new EqualsBuilder()
				.append(this.name, other.name)
				.append(this.toolId, other.toolId)
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder()
				.append(this.name)
				.append(this.toolId)
				.toHashCode();
	}

}
