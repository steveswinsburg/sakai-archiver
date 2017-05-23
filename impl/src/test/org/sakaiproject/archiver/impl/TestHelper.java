package org.sakaiproject.archiver.impl;

import java.util.Date;

import org.sakaiproject.archiver.api.Status;
import org.sakaiproject.archiver.entity.ArchiveEntity;

/**
 * Helper methods for tests
 */
public class TestHelper {

	public static ArchiveEntity mockArchiveEntity(final String archiveId, final String siteId, final String userUuid, final Date startDate,
			final Date endDate, final Status status, final String zipPath) {
		final ArchiveEntity entity = new ArchiveEntity();
		entity.setId(archiveId);
		entity.setSiteId(siteId);
		entity.setUserUuid(userUuid);
		entity.setStartDate(startDate);
		entity.setEndDate(endDate);
		entity.setStatus(status);
		entity.setZipPath(zipPath);
		return entity;
	}
}
