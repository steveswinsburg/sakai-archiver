package org.sakaiproject.archiver.impl;

import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.dto.Archive;
import org.sakaiproject.archiver.persistence.ArchiverPersistenceService;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ArchiverServiceImpl implements ArchiverService {

	@Setter
	private ArchiverPersistenceService dao;

	public void init() {
		log.info("ArchiverService started");
	}

	@Override
	public boolean isArchiveInProgress(final String siteId) {
		final Archive archive = this.dao.getCurrent(siteId);
		return (archive != null) ? true : false;
	}

	@Override
	public void startArchive(final String siteId, final String userUuid, final boolean includeStudentData, final String... toolIds) {

		this.dao.create(siteId, userUuid);

	}
}
