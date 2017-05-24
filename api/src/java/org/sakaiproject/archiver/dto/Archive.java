package org.sakaiproject.archiver.dto;

import java.util.Date;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
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

public class Archive {

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

}
