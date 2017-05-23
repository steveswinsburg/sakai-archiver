package org.sakaiproject.archiver.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.archiver.api.Status;
import org.sakaiproject.archiver.entity.ArchiveEntity;
import org.sakaiproject.archiver.persistence.ArchiverPersistenceService;

@RunWith(MockitoJUnitRunner.class)
public class ArchiverServiceImplTest {

	@Mock
	private ArchiverPersistenceService dao;

	@InjectMocks
	private ArchiverServiceImpl impl;

	@Test
	public final void should_returnTrue_when_ArchiveIsInProgress() {
		final String archiveId = UUID.randomUUID().toString();
		final String siteId = UUID.randomUUID().toString();
		final String userUuid = UUID.randomUUID().toString();
		final Date startDate = new Date();
		final Date endDate = null;
		final Status status = Status.STARTED;
		final String zipPath = null;

		final ArchiveEntity entity = TestHelper.mockArchiveEntity(archiveId, siteId, userUuid, startDate, endDate, status, zipPath);

		when(this.dao.getCurrent(anyString())).thenReturn(entity);
		final boolean result = this.impl.isArchiveInProgress(anyString());
		assertTrue("Archive should be in progress", result);
	}

	@Test
	public final void should_returnFalse_when_ArchiveIsNotInProgress() {

		when(this.dao.getCurrent(anyString())).thenReturn(null);
		final boolean result = this.impl.isArchiveInProgress(anyString());
		assertFalse("Archive should not be in progress", result);
	}

}
