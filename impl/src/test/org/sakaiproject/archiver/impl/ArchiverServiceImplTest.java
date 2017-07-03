package org.sakaiproject.archiver.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


import java.util.Date;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.archiver.api.Status;
import org.sakaiproject.archiver.dto.Archive;
import org.sakaiproject.archiver.entity.ArchiveEntity;
import org.sakaiproject.archiver.persistence.ArchiverPersistenceService;

@RunWith(MockitoJUnitRunner.class)
public class ArchiverServiceImplTest {

	@Mock
	private ArchiverPersistenceService dao;

	@InjectMocks
	private ArchiverServiceImpl impl;

	@Test
	public final void should_returnArchive_when_ArchiveExistsForSite() {
		final String archiveId = UUID.randomUUID().toString();
		final String siteId = UUID.randomUUID().toString();
		final String userUuid = UUID.randomUUID().toString();
		final Date startDate = new Date();
		final Date endDate = null;
		final Status status = Status.STARTED;
		final String zipPath = null;

		final ArchiveEntity entity = TestHelper.mockArchiveEntity(archiveId, siteId, userUuid, startDate, endDate, status, zipPath);

		when(this.dao.getLatest(anyString())).thenReturn(entity);
		Archive archive = this.impl.getLatest(anyString());
		
		assertNotNull("Archive should be in progress", archive);
	}

	@Test
	public final void should_returnNull_when_ArchiveNotStartedForSite() {

		when(this.dao.getLatest(anyString())).thenReturn(null);		
		Archive archive = this.impl.getLatest(anyString());
		assertNull("Archive should be in progress", archive);
	}

}
