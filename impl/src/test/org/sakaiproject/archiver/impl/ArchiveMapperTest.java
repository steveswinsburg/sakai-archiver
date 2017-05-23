package org.sakaiproject.archiver.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Date;
import java.util.UUID;

import org.junit.Test;
import org.sakaiproject.archiver.api.Status;
import org.sakaiproject.archiver.dto.Archive;
import org.sakaiproject.archiver.entity.ArchiveEntity;

public class ArchiveMapperTest {

	@Test
	public final void should_mapToDto_whenDataProvided() {

		final String archiveId = UUID.randomUUID().toString();
		final String siteId = UUID.randomUUID().toString();
		final String userUuid = UUID.randomUUID().toString();
		final Date startDate = new Date();
		final Date endDate = new Date();
		final Status status = Status.STARTED;
		final String zipPath = "/path/to/zip/file.zip";

		final ArchiveEntity entity = TestHelper.mockArchiveEntity(archiveId, siteId, userUuid, startDate, endDate, status, zipPath);

		final Archive dto = ArchiveMapper.toDto(entity);

		assertEquals("archiveId not mapped correctly", entity.getId(), dto.getArchiveId());
		assertEquals("siteId not mapped correctly", entity.getStartDate(), dto.getStartDate());
		assertEquals("userUuid not mapped correctly", entity.getUserUuid(), dto.getUserUuid());
		assertEquals("startDate not mapped correctly", entity.getStartDate(), dto.getStartDate());
		assertEquals("endDate not mapped correctly", entity.getEndDate(), dto.getEndDate());
		assertEquals("status not mapped correctly", entity.getStatus(), dto.getStatus());
		assertEquals("zipPath not mapped correctly", entity.getZipPath(), dto.getZipPath());
	}

	@Test
	public final void should_returnNull_whenEntityIsNull() {

		final ArchiveEntity entity = null;
		final Archive dto = ArchiveMapper.toDto(entity);
		assertNull("dto should be null", dto);
	}

}
