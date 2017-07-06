package org.sakaiproject.archiver.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.GenericGenerator;
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
@Entity
@Table(name = "archiver_archives")
public class ArchiveEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	@Getter
	@Setter
	@Id
	@Column(name = "id", length = 36)
	@GeneratedValue(generator = "uuid")
	@GenericGenerator(name = "uuid", strategy = "uuid2")
	private String id;

	@Getter
	@Setter
	@Column(name = "site_id", length = 50, nullable = false)
	private String siteId;

	@Getter
	@Setter
	@Column(name = "user_uuid", length = 50, nullable = false)
	private String userUuid;

	@Getter
	@Setter
	@Column(name = "settings", nullable = true)
	@Lob
	private String settings;

	@Getter
	@Setter
	@Column(name = "status", nullable = true)
	@Enumerated(EnumType.STRING)
	private Status status;

	@Getter
	@Setter
	@Column(name = "archive_path", length = 2000, nullable = true)
	private String archivePath;

	@Getter
	@Setter
	@Column(name = "zip_path", length = 2000, nullable = true)
	private String zipPath;

	@Getter
	@Setter
	@Column(name = "start_date", nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	private Date startDate;

	@Getter
	@Setter
	@Column(name = "end_date", nullable = true)
	@Temporal(TemporalType.TIMESTAMP)
	private Date endDate;

}
