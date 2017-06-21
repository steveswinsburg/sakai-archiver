package org.sakaiproject.archiver.dto;

import java.io.Serializable;
import java.util.Date;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.sakaiproject.archiver.api.Status;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO for an archive
 *
 * @since 12.0
 * @author Steve Swinsburg (steve.swinsburg@gmail.com)
 *
 */
public class Archive implements Comparable<Archive>, Serializable {

	private static final long serialVersionUID = 1L;

	@Getter
	@Setter
	private String archiveId;

	@Getter
	@Setter
	private String siteId;

	@Getter
	@Setter
	private String userUuid;

	@Getter
	@Setter
	private Date startDate;

	@Getter
	@Setter
	private Date endDate;

	@Getter
	@Setter
	private Status status;

	@Getter
	@Setter
	private String zipPath;

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}

	@Override
	public int compareTo(final Archive other) {
		return new CompareToBuilder()
				.append(this.startDate, other.startDate)
				.append(this.endDate, other.endDate)
				.append(this.status, other.status)
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
		final Archive other = (Archive) o;
		return new EqualsBuilder()
				.append(this.archiveId, other.archiveId)
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.archiveId).toHashCode();
	}

}
