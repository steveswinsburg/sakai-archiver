package org.sakaiproject.archiver.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.sakaiproject.archiver.api.Archiveable;
import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.dto.Archive;
import org.sakaiproject.archiver.exception.ArchiveAlreadyInProgressException;
import org.sakaiproject.archiver.exception.ToolsNotSpecifiedException;
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
	public void startArchive(final String siteId, final String userUuid, final boolean includeStudentData, final String... toolIds)
			throws ToolsNotSpecifiedException, ArchiveAlreadyInProgressException {

		// check tools
		final List<String> toolsToArchive = Arrays.asList(toolIds);

		if (toolsToArchive.isEmpty()) {
			throw new ToolsNotSpecifiedException("Tools not specified");
		}

		if (isArchiveInProgress(siteId)) {
			throw new ArchiveAlreadyInProgressException("An archive is already in progress for this site");
		}

		// create the record
		final Archive archive = this.dao.create(siteId, userUuid);

		// now archive all of the registered tools
		// TODO this must run in a separate thread
		final Map<String, Archiveable> registry = ArchiverRegistry.getInstance().getRegistry();

		for (final String toolId : toolsToArchive) {
			final Archiveable archivable = registry.get(toolId);
			if (archivable == null) {
				log.error("No registered archiver for %S", toolId);
				break;
			}

			archivable.archive(archive.getArchiveId(), includeStudentData);
		}

	}

	@Override
	public void archiveContent(final String archiveId, final String toolId, final byte[] content, final String filename) {

		log.info("Archiving to %S for %S as %S with content of: %S", archiveId, toolId, filename, content.toString());

	}
}
