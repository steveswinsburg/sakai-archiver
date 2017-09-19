package org.sakaiproject.archiver.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
import org.sakaiproject.archiver.exception.ArchiveCancellationException;
import org.sakaiproject.archiver.exception.ArchiveCompletionException;
import org.sakaiproject.archiver.exception.ArchiveInitialisationException;
import org.sakaiproject.archiver.exception.ArchiveNotFoundException;
import org.sakaiproject.archiver.exception.ArchiveProcessingException;
import org.sakaiproject.archiver.exception.FileExtensionExcludedException;
import org.sakaiproject.archiver.exception.FileSizeExceededException;
import org.sakaiproject.archiver.exception.ToolsNotSpecifiedException;
import org.sakaiproject.archiver.persistence.ArchiverPersistenceService;
import org.sakaiproject.archiver.spi.Archiveable;
import org.sakaiproject.archiver.util.Zipper;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SitePage;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.Tool;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ArchiverServiceImpl implements ArchiverService {

	@Setter
	private ArchiverPersistenceService dao;

	@Setter
	private ServerConfigurationService serverConfigurationService;

	@Setter
	private UserDirectoryService userDirectoryService;

	@Setter
	private SessionManager sessionManager;

	@Setter
	private AuthzGroupService authzGroupService;

	@Setter
	private SiteService siteService;

	@Setter
	private ToolManager toolManager;

	@Setter
	private SecurityService securityService;

	public void init() {
		log.info("ArchiverService started");
	}

	@Override
	public void startArchive(final String siteId, final String userUuid, final boolean includeStudentData, final String... toolIds)
			throws ToolsNotSpecifiedException, ArchiveAlreadyInProgressException, ArchiveInitialisationException,
			ArchiveCompletionException {

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

		final Map<String, List<Archiveable>> registry = ArchiverRegistry.getInstance().getRegistry();

		final ExecutorService taskExecutor = Executors.newSingleThreadExecutor();

		final List<Future<Status>> futures = new ArrayList<>();

		final User currentUser = this.userDirectoryService.getCurrentUser();

		// archive the requested toolIds
		for (final String toolId : toolsToArchive) {
			final List<Archiveable> archiveables = registry.get(toolId);
			if (archiveables == null || archiveables.isEmpty()) {
				log.error("No registered archivers for {}", toolId);
				return;
			}

			for (final Archiveable archiveable : archiveables) {

				final Callable<Status> task = () -> {

					log.info("Archiving {} with provider {}", toolId, archiveable.getClass().getCanonicalName());

					injectUser(currentUser);

					try {
						archiveable.archive(archiveId, siteId, includeStudentData);
						return Status.COMPLETE;
					} catch (final Exception e) {
						log.error(
								"An exception occurred whilst archiving content for site {} and tool {}. The archive may be incomplete.",
								siteId, toolId, e);
						return Status.INCOMPLETE;
					}
				};

				// non blocking invocation
				futures.add(taskExecutor.submit(task));

			}
		}

		taskExecutor.shutdown();

		// spin up another executor for the finalise to use
		final ExecutorService finaliseExecutor = Executors.newSingleThreadExecutor();
		final Callable<Void> finaliseTask = () -> {

			log.debug("Waiting for all archiving threads to finish...");

			// TODO this should be a list of statuses as the last one could be ok but one of them failed and it will remain at STARTED.
			// Also means we can do better reporting
			Status status = Status.COMPLETE;
			for (final Future<Status> future : futures) {
				// wait for each to finish and check the status
				status = future.get();
			}

			log.debug("All archiving threads are complete, finalising the archive.");

			finalise(entity, status);

			return null;
		};

		finaliseExecutor.submit(finaliseTask);
		finaliseExecutor.shutdown();
	}

	@Override
	public void archiveContent(final String archiveId, final String siteId, final String toolId, final byte[] content,
			final String filename) {
		archiveContent(archiveId, siteId, toolId, content, filename, "");
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
		final String filePath = buildPath(getArchiveBasePath(), siteId, archiveId, sanitise(toolId), buildPath(sanitise(subdirectories)),
				sanitise(filename));
		writeFile(content, filePath);

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
	public List<Archive> getArchives(final String siteId, final int max) {
		if (StringUtils.isBlank(siteId) && !isSuperUser()) {
			return Collections.emptyList();
		}
		final List<ArchiveEntity> entities = this.dao.getBySiteId(siteId, max);
		return ArchiveMapper.toDtos(entities);
	}

	@Override
	public Archive getLatest(final String siteId) {
		final ArchiveEntity entity = this.dao.getLatest(siteId);
		if (entity == null) {
			log.debug("No archive exists for siteId {}", siteId);
			return null;
		}

		return ArchiveMapper.toDto(entity);
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
	 * Build the index page
	 *
	 * @param entity
	 */
	private void buildIndex(final String archivePath) {

		final IndexBuilder indexBuilder = new IndexBuilder(archivePath);
		final String indexHtml = indexBuilder.build();

		log.debug("Writing index.html: {}", indexHtml);

		final String indexPath = buildPath(archivePath, "index.html");
		writeFile(indexHtml.getBytes(), indexPath);
	}

	/**
	 * Finalise an archiving record with the specified status.
	 *
	 * Also builds the index page.
	 *
	 * Note that the passed in status could be overridden if an error occurs in finalising the archive.
	 *
	 * @param entity the {@link ArchiveEntity} tracking this archive
	 * @param status the {@link Status} to set
	 */
	private void finalise(final ArchiveEntity entity, final Status status) {

		buildIndex(entity.getArchivePath());

		final String zipName = sanitise(getSiteTitle(entity.getSiteId()) + "-" + entity.getId());

		// zips the archive directory
		final File archiveDirectory = new File(entity.getArchivePath());
		try {
			final String zipPath = Zipper.zipDirectory(archiveDirectory, zipName);
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
	 * Checks if an archive is in progress for the given site.
	 *
	 * @return true/false
	 */
	private boolean isArchiveInProgress(final String siteId) {

		final ArchiveEntity entity = this.dao.getLatest(siteId);
		if (entity == null) {
			return false;
		}
		return (entity.getStatus() == Status.STARTED);
	}

	/**
	 * Inject user into the session so that Sakai permission checks will be happy
	 *
	 * @param user the user to inject into the session
	 *
	 * @throws ArchiveInitialisationException if the archive could not
	 */
	private void injectUser(final User user) throws ArchiveInitialisationException {

		if (user == null) {
			throw new ArchiveInitialisationException("Archive session could not be updated. Archiver cannot be run.");
		}

		Session session = this.sessionManager.getCurrentSession();
		if (session == null) {
			session = this.sessionManager.startSession();
		}

		log.debug("Injecting session {} with user {}", session.getId(), user.getEid());

		session.setUserId(user.getId());
		session.setActive();
		this.sessionManager.setCurrentSession(session);

		// TODO is this line strictly necessary?
		this.authzGroupService.refreshUser(user.getId());

		log.debug("Session ready");
	}

	@Override
	public String getSiteHeader(final String siteId, final String toolId) {

		final Site site = getSite(siteId);
		final String siteTitle = getSiteTitle(siteId);
		final String toolName = getToolName(siteId, toolId);

		final ResourceProperties props = site.getProperties();
		final String term = (String) props.get(Site.PROP_SITE_TERM);
		if (term != null) {
			return String.format("%s (%s): %s", siteTitle, term, toolName);
		} else {
			return String.format("%s: %s", siteTitle, toolName);
		}

	}

	@Override
	public String getToolName(final String siteId, final String toolId) {
		final Site site = getSite(siteId);

		String name = null;

		// find the page and grab the toolname from that.
		for (final SitePage page : site.getPages()) {
			for (final ToolConfiguration toolConfig : page.getTools()) {
				if (StringUtils.equals(toolId, toolConfig.getToolId())) {
					name = page.getTitle();
				}
			}
		}
		// try the tool name or fall back to toolId
		if (StringUtils.isBlank(name)) {
			final Tool t = this.toolManager.getTool(toolId);
			name = (t != null) ? t.getTitle() : toolId;
		}
		return name;
	}

	@Override
	public void cancelArchive(final String archiveId) throws ArchiveCancellationException {
		final ArchiveEntity entity = this.dao.getByArchiveId(archiveId);
		if (entity.getStatus() != Status.STARTED) {
			throw new ArchiveCancellationException("Archive could not be cancelled as it is not in the STARTED state.");
		}
		entity.setStatus(Status.CANCELLED);
		entity.setEndDate(new Date());
		this.dao.update(entity);

		// note that this does not cleanup any files that were produced nor kill the thread if it is still running. This was out of scope
		// but would be good to do right here.
	}

	/**
	 * Get the site
	 *
	 * @param siteId
	 * @throws ArchiveProcessingException if error occurs getting the site
	 * @return
	 */
	private Site getSite(final String siteId) {
		try {
			return this.siteService.getSite(siteId);
		} catch (final IdUnusedException e) {
			// this should never occur as we are dealing with a site that does exist
			throw new ArchiveProcessingException("Site could not be found", e);
		}
	}

	/**
	 * Get the title of a site
	 *
	 * @param siteId
	 * @return
	 */
	private String getSiteTitle(final String siteId) {
		final Site site = getSite(siteId);
		return site.getTitle();
	}

	/**
	 * Check if the user is a superuser
	 *
	 * @return
	 */
	private boolean isSuperUser() {
		final User user = this.userDirectoryService.getCurrentUser();
		return this.securityService.isSuperUser(user.getId());
	}

	/**
	 * Replace illegal chars in the supplied string with _
	 *
	 * @param filename
	 * @return
	 */
	private String sanitise(final String string) {
		return string.replaceAll("[^a-zA-Z0-9.-]", "_");
	}

	/**
	 * Replace illegal chars in the supplied strings with _
	 *
	 * @param filename varargs
	 * @return
	 */
	private String[] sanitise(final String... strings) {
		return Arrays.stream(strings)
				.map(s -> sanitise(s))
				.toArray(String[]::new);
	}

	/**
	 * Write the given byte[] to a file. Makes no guarantee that this will work and will log if it doesn't. Don't rely on it.
	 *
	 * @param content byte[] to write
	 * @param filePath full path to write to
	 */
	private void writeFile(final byte[] content, final String filePath) {
		log.debug("Writing to {}", filePath);

		final File file = new File(filePath);

		try {
			FileUtils.writeByteArrayToFile(file, content);
		} catch (final IOException e) {
			log.error("Could not write file: " + file, e);
		}
	}

}
