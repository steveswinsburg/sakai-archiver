package org.sakaiproject.archiver.impl;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.sakaiproject.archiver.api.Archiveable;
import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.api.Status;
import org.sakaiproject.archiver.dto.Archive;
import org.sakaiproject.archiver.entity.ArchiveEntity;
import org.sakaiproject.archiver.exception.ArchiveAlreadyInProgressException;
import org.sakaiproject.archiver.exception.ArchiveInitialisationException;
import org.sakaiproject.archiver.exception.ToolsNotSpecifiedException;
import org.sakaiproject.archiver.persistence.ArchiverPersistenceService;
import org.sakaiproject.archiver.util.Zipper;
import org.sakaiproject.component.api.ServerConfigurationService;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ArchiverServiceImpl implements ArchiverService {

	@Setter
	private ArchiverPersistenceService dao;

	@Setter
	private ServerConfigurationService serverConfigurationService;

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
			throws ToolsNotSpecifiedException, ArchiveAlreadyInProgressException, ArchiveInitialisationException {

		// validate
		final List<String> toolsToArchive = Arrays.asList(toolIds);

		if (toolsToArchive.isEmpty()) {
			throw new ToolsNotSpecifiedException("Tools not specified");
		}

		if (isArchiveInProgress(siteId)) {
			throw new ArchiveAlreadyInProgressException("An archive is already in progress for this site");
		}

		// create the record
		final ArchiveEntity archive = this.dao.create(siteId, userUuid);
		final String archiveId = archive.getId();

		// create disk location
		final String archivePath = buildPath(getArchiveBasePath(), siteId, archiveId);
		try {
			FileUtils.forceMkdir(FileUtils.getFile(archivePath));
		} catch (final IOException e) {
			throw new ArchiveInitialisationException("Archive could not be started", e);
		}

		// update archive with file location
		archive.setArchivePath(archivePath);
		this.dao.update(archive);

		// now archive all of the registered tools
		// TODO this must run in a separate thread and return a Future that the finalise will use when this thread is done
		final Map<String, Archiveable> registry = ArchiverRegistry.getInstance().getRegistry();

		for (final String toolId : toolsToArchive) {
			final Archiveable archivable = registry.get(toolId);
			if (archivable == null) {
				log.error("No registered archiver for {}", toolId);
				break;
			}

			archivable.archive(archiveId, siteId, includeStudentData);
		}

		finalise(archive);
	}

	@Override
	public void archiveContent(final String archiveId, final String toolId, final byte[] content, final String filename) {
		archiveContent(archiveId, toolId, content, null, filename);
	}

	@Override
	public void archiveContent(final String archiveId, final String toolId, final byte[] content, final String subdirectory,
			final String filename) {
		log.info("Archiving to {} for {} as {} inside {} with content of: {}", archiveId, toolId, filename, subdirectory,
				content.toString());
	}

	/**
	 * Get base dir for all archives as configured in sakai.properties
	 *
	 * @return
	 */
	private String getArchiveBasePath() {
		return this.serverConfigurationService.getString("archiver.path", FileUtils.getTempDirectoryPath());
	}

	/**
	 * Build a path made up of the parts supplied, and using the system independent file separator
	 */
	private String buildPath(final String... parts) {
		return String.join(File.separator, parts);
	}

	/**
	 * Finalise an archive
	 *
	 * @param archive the ArchiveEntity tracking this archive
	 */
	private void finalise(final ArchiveEntity archive) {

		// zips the archive directory
		final File archiveDirectory = new File(archive.getArchivePath());
		try {
			final String zipPath = Zipper.zipDirectory(archiveDirectory);
			archive.setZipPath(zipPath);
			archive.setStatus(Status.COMPLETE);
		} catch (final IOException e) {
			log.error("Could not zip archive");
			archive.setStatus(Status.FAILED);
		}

		// close the db record
		archive.setEndDate(new Date());
		this.dao.update(archive);
	}

}
