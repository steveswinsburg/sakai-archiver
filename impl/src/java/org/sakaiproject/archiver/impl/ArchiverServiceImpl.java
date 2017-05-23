package org.sakaiproject.archiver.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.sakaiproject.archiver.api.Archiveable;
import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.api.Status;
import org.sakaiproject.archiver.entity.ArchiveEntity;
import org.sakaiproject.archiver.exception.ArchiveAlreadyInProgressException;
import org.sakaiproject.archiver.exception.ArchiveInitialisationException;
import org.sakaiproject.archiver.exception.FileExtensionExcludedException;
import org.sakaiproject.archiver.exception.FileSizeExceededException;
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

		final ArchiveEntity archive = this.dao.getCurrent(siteId);
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
				continue;
			}

			log.info("Archiving {}", toolId);
			archivable.archive(archiveId, siteId, toolId, includeStudentData);
		}

		finalise(archive);
	}

	@Override
	public void archiveContent(final String archiveId, final String siteId, final String toolId, final byte[] content,
			final String filename) {
		archiveContent(archiveId, siteId, toolId, content, filename, new String[0]);
	}

	@Override
	public void archiveContent(final String archiveId, final String siteId, final String toolId, final byte[] content,
			final String filename, final String... subdirectories) {
		log.debug("Archiving to archive: {} for site: {} and tool: {} in dir: {} and file: {} with content: {}", archiveId, siteId, toolId,
				buildPath(subdirectories), filename,
				Arrays.toString(subdirectories));

		try {
			validateFileExtension(filename);
		} catch (final FileExtensionExcludedException e1) {
			log.error("File {} is of an excluded extension and will not be archived", filename);
		}

		try {
			validateFileSize(content);
		} catch (final FileSizeExceededException e1) {
			log.error("File {} is too large and will not be archived", filename);
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
		final long maxBytes = config * FileUtils.ONE_MB;
		return maxBytes;
	}

	/**
	 * Get any excluded extensions as configured in sakai.properties via <code>archiver.exclude.extensions</code>
	 *
	 * @return
	 */
	private List<String> getExcludedExtensions() {
		final List<String> excludedExtensions = new ArrayList<>();
		final String[] config = this.serverConfigurationService.getStrings("archiver.exclude.extensions");
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

	/**
	 * Validate the length of a file to be archived
	 *
	 * @param content
	 * @throws FileSizeExceededException
	 */
	private void validateFileSize(final byte[] content) throws FileSizeExceededException {
		if (content.length > getMaxFileSize()) {
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
		if (getExcludedExtensions().contains(extension)) {
			throw new FileExtensionExcludedException();
		}
	}

}
