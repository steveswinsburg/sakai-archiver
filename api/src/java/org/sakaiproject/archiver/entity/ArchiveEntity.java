package org.sakaiproject.archiver.entity;

import java.io.Serializable;
import java.util.Date;

import org.sakaiproject.archiver.api.Status;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Persistent entity for the archive records
 *
 * For 10.4 these annotations are unused. See ArchiveEntity.hbm.xml instead.
 *
 * @since 12.0
 * @author Steve Swinsburg (steve.swinsburg@gmail.com)
 */
@ToString(includeFieldNames = true)
public class ArchiveEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	@Getter
	@Setter
	private String id;

	@Getter
	@Setter
	private String siteId;

	@Getter
	@Setter
	private String userUuid;

	@Getter
	@Setter
	private String settings;

	@Getter
	@Setter
	private Status status;

	@Getter
	@Setter
	private String archivePath;

	@Getter
	@Setter
	private String zipPath;

	@Getter
	@Setter
	private Date startDate;

	@Getter
	@Setter
	private Date endDate;

	public ArchiveEntity() {
	}

}
