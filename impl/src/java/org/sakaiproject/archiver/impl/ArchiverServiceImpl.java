package org.sakaiproject.archiver.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.api.Status;
import org.sakaiproject.archiver.dto.Archive;
import org.sakaiproject.archiver.entity.ArchiveEntity;
import org.sakaiproject.archiver.exception.ArchiveAlreadyInProgressException;
import org.sakaiproject.archiver.exception.ArchiveInitialisationException;
import org.sakaiproject.archiver.exception.ArchiveNotFoundException;
import org.sakaiproject.archiver.exception.FileExtensionExcludedException;
import org.sakaiproject.archiver.exception.FileSizeExceededException;
import org.sakaiproject.archiver.exception.ToolsNotSpecifiedException;
import org.sakaiproject.archiver.persistence.ArchiverPersistenceService;
import org.sakaiproject.archiver.spi.Archiveable;
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

		final ArchiveEntity entity = this.dao.getCurrent(siteId);
		return (entity != null) ? true : false;
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
		final ArchiveEntity entity = this.dao.create(siteId, userUuid);
		final String archiveId = entity.getId();

		// create disk location
		final String archivePath = buildPath(getArchiveBasePath(), siteId, archiveId);
		try {
			FileUtils.forceMkdir(FileUtils.getFile(archivePath));
		} catch (final IOException e) {
			throw new ArchiveInitialisationException("Archive could not be started", e);
		}

		// update archive with file location
		entity.setArchivePath(archivePath);
		this.dao.update(entity);

		// TODO this must run in a separate thread and return a Future that the finalise will use when this thread is done
		Status status = Status.COMPLETE;

		final List<String> customRegistrations = getCustomRegistrations();

		final List<String> allRegistrations = new ArrayList<>();
		allRegistrations.addAll(toolsToArchive);

		// this needs to ensure it is tied to a tool. so needs some work in the archiverregistry
		allRegistrations.addAll(customRegistrations);

		final Map<String, Archiveable> registry = ArchiverRegistry.getInstance().getRegistry();

		// archive all registered and custom tools
		for (final String toolId : allRegistrations) {
			final Archiveable archivable = registry.get(toolId);
			if (archivable == null) {
				log.error("No registered archiver for {}", toolId);
				return;
			}

			log.info("Archiving {}", toolId);

			try {
				archivable.archive(archiveId, siteId, toolId, includeStudentData);
			} catch (final RuntimeException e) {
				// TODO rethrow as a checked exception which the UI can deal with
				log.error("A runtime exception occurred whilst archiving content for site {} and tool {}. The archive may be incomplete.",
						siteId, toolId, e);
				status = Status.INCOMPLETE;
			}
		}

		finalise(entity, status);
	}

	@Override
	public void archiveContent(final String archiveId, final String siteId, final String toolId, final byte[] content,
			final String filename) {
		archiveContent(archiveId, siteId, toolId, content, filename, new String[0]);
	}

	@Override
	public void archiveContent(final String archiveId, final String siteId, final String toolId, final byte[] content,
			final String filename, final String... subdirectories) {
		log.debug("Archiving to archive: {} for site: {} and tool: {} in dir: {} and file: {}", archiveId, siteId, toolId,
				buildPath(subdirectories), filename);

		try {
			validateFileExtension(filename);
		} catch (final FileExtensionExcludedException e1) {
			log.error("File {} is of an excluded extension and will not be archived", filename);
			return;
		}

		try {
			validateFileSize(content);
		} catch (final FileSizeExceededException e1) {
			log.error("File {} is too large and will not be archived", filename);
			return;
		}

		// TODO perhaps turn this into a validate?
		if (ArrayUtils.isEmpty(content)) {
			log.error("No content to archive. Skipping.");
			return;
		}

		// archive-base/siteId/archiveId/toolId/[subdirs]/file
		final String filePath = buildPath(getArchiveBasePath(), siteId, archiveId, toolId, buildPath(subdirectories), filename);
		log.debug("Writing to {}", filePath);

		final File file = new File(filePath);

		try {
			FileUtils.writeByteArrayToFile(file, content);
		} catch (final IOException e) {
			log.error("Could not write file: " + file, e);
		}

	}

	@Override
	public Archive getArchive(final String archiveId) throws ArchiveNotFoundException {
		final ArchiveEntity entity = this.dao.getByArchiveId(archiveId);
		if (entity == null) {
			throw new ArchiveNotFoundException("Archive " + archiveId + " does not exist");
		}

		return ArchiveMapper.toDto(entity);
	}

	@Override
	public List<Archive> getArchives(final String siteId) {
		if (StringUtils.isBlank(siteId)) {
			return Collections.emptyList();
		}
		final List<ArchiveEntity> entities = this.dao.getBySiteId(siteId);
		return ArchiveMapper.toDtos(entities);
	}

	/**
	 * Get base dir for all archives as configured in sakai.properties via <code>archiver.path</code>
	 *
	 * Removes any trailing /
	 *
	 * Note: This may need adjusting on Windows?
	 *
	 * @return
	 */
	private String getArchiveBasePath() {
		return StringUtils.removeEnd(this.serverConfigurationService.getString("archiver.path", FileUtils.getTempDirectoryPath()), "/");
	}

	/**
	 * Get the maximum individual filesize (in bytes) that can be included in an archive as configured in sakai.properties via
	 * <code>archiver.max.filesize</cope> which is a number in MB.
	 *
	 * Default if not specified is: 50MB = 52,428,800
	 *
	 * @return
	 */
	private long getMaxFileSize() {
		final int config = this.serverConfigurationService.getInt("archiver.max.filesize", 50);
		return config * FileUtils.ONE_MB;
	}

	/**
	 * Get any excluded extensions as configured in sakai.properties via <code>archiver.excluded.extensions</code>
	 *
	 * Default is none.
	 *
	 * @return
	 */
	private List<String> getExcludedExtensions() {
		final List<String> excludedExtensions = new ArrayList<>();
		final String[] config = this.serverConfigurationService.getStrings("archiver.excluded.extensions");
		if (ArrayUtils.isNotEmpty(config)) {
			excludedExtensions.addAll(Arrays.asList(config));
		}
		return excludedExtensions;
	}

	/**
	 * Build a path made up of the parts supplied, and using the system's file separator. Null safe in both array and element.
	 */
	private String buildPath(final String... parts) {
		if (parts == null || parts.length == 0) {
			return null;
		}

		final String[] cleanedParts = Arrays.stream(parts)
				.filter(s -> (StringUtils.isNotBlank(s)))
				.toArray(String[]::new);

		return String.join(File.separator, cleanedParts);
	}

	/**
	 * Finalise an archiving record with the specified status.
	 *
	 * Note that the status could be overridden if an error occurs in finalising the archive.
	 *
	 * @param entity the {@link ArchiveEntity} tracking this archive
	 * @param status the {@link Status} to set
	 */
	private void finalise(final ArchiveEntity entity, final Status status) {

		// zips the archive directory
		final File archiveDirectory = new File(entity.getArchivePath());
		try {
			final String zipPath = Zipper.zipDirectory(archiveDirectory);
			entity.setZipPath(zipPath);
			entity.setStatus(status);
		} catch (final IOException e) {
			log.error("Could not zip archive");
			entity.setStatus(Status.FAILED);
		}

		// close the db record
		entity.setEndDate(new Date());
		this.dao.update(entity);
	}

	/**
	 * Validate the length of a file to be archived
	 *
	 * @param content
	 * @throws FileSizeExceededException
	 */
	private void validateFileSize(final byte[] content) throws FileSizeExceededException {

		int size = 0;
		if (ArrayUtils.isNotEmpty(content)) {
			size = content.length;
		}
		log.debug("File size: {}", size);

		if (size > getMaxFileSize()) {
			throw new FileSizeExceededException();
		}
	}

	/**
	 * Valite the file extension is not excluded
	 *
	 * @param filename
	 * @throws FileSizeExceededException
	 */
	private void validateFileExtension(final String filename) throws FileExtensionExcludedException {
		final String extension = FilenameUtils.getExtension(filename);
		log.debug("File extension: {}", extension);
		if (getExcludedExtensions().contains(extension)) {
			throw new FileExtensionExcludedException();
		}
	}

	/**
	 * Get a list of custom registrations, ie those that start with {@link Archiveable#CUSTOM_PREFIX}
	 *
	 * @return
	 */
	private List<String> getCustomRegistrations() {
		final Map<String, Archiveable> registry = ArchiverRegistry.getInstance().getRegistry();
		return registry.keySet().stream().filter(k -> StringUtils.startsWith(k, Archiveable.CUSTOM_PREFIX)).collect(Collectors.toList());
	}

}
