package org.sakaiproject.archiver.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Persistent entity for the archive records
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
	@Column(name = "id")
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "archive_entity_sequence")
	@SequenceGenerator(name = "archive_entity_sequence", sequenceName = "archive_entity_seq")
	private long id;
	
	@Getter
	@Setter
	@Column(name = "site_id", length=50, nullable = false)
	private String siteId;
	
	@Getter
	@Setter
	@Column(name = "user_uuid", length=50, nullable = false)
	private String userUuid;
	
	@Getter
	@Setter
	@Column(name = "settings", nullable = false)
	@Lob
	private String settings;
	
	@Getter
	@Setter
	@Column(name = "archive_id", length=200, nullable = false)
	private String archiveId;
	
	@Getter
	@Setter
	@Column(name = "start_date", nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	private Date startDate;
	
	@Getter
	@Setter
	@Column(name = "end_date", nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	private Date endDate;
	
	
}
