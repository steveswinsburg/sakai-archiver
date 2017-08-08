package org.sakaiproject.archiver.app.model;

import java.io.Serializable;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import lombok.Getter;

/**
 * Wrapper for a Sakai site holding just the morsels of data that we need
 */
public class ArchiveSite implements Serializable, Comparable<ArchiveSite> {

	private static final long serialVersionUID = 1L;

	@Getter
	private String siteId;

	@Getter
	private String title;

	public ArchiveSite() {
	}

	public ArchiveSite(final String siteId, final String title) {
		this.siteId = siteId;
		this.title = title;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}

	@Override
	public int compareTo(final ArchiveSite o) {
		return new CompareToBuilder()
				.append(this.siteId, o.siteId)
				.append(this.title, o.title)
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
		final ArchiveSite other = (ArchiveSite) o;
		return new EqualsBuilder()
				.append(this.siteId, other.siteId)
				.append(this.title, other.title)
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder()
				.append(this.siteId)
				.append(this.title)
				.toHashCode();
	}

}
