package org.sakaiproject.archiver.app.business;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.api.Status;
import org.sakaiproject.archiver.app.model.ArchiveSettings;
import org.sakaiproject.archiver.app.model.ArchiveSite;
import org.sakaiproject.archiver.app.model.ArchiveableTool;
import org.sakaiproject.archiver.dto.Archive;
import org.sakaiproject.archiver.exception.ArchiveAlreadyInProgressException;
import org.sakaiproject.archiver.exception.ArchiveCancellationException;
import org.sakaiproject.archiver.exception.ArchiveCompletionException;
import org.sakaiproject.archiver.exception.ArchiveInitialisationException;
import org.sakaiproject.archiver.exception.ArchiveNotFoundException;
import org.sakaiproject.archiver.exception.ToolsNotSpecifiedException;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SitePage;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.SiteService.SelectionType;
import org.sakaiproject.site.api.SiteService.SortType;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.util.ResourceLoader;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Business service for the Archiver app
 *
 * This is not designed to be consumed outside of the application.
 *
 * @author Steve Swinsburg (steve.swinsburg@gmail.com)
 */
@Slf4j
public class ArchiverBusinessService {

	@Setter
	private SiteService siteService;

	@Setter
	private UserDirectoryService userDirectoryService;

	@Setter
	private ToolManager toolManager;

	@Setter
	private SecurityService securityService;

	@Setter
	private ServerConfigurationService serverConfigurationService;

	@Setter
	private ArchiverService archiverService;

	/**
	 * Helper to get user
	 *
	 * @return
	 */
	public User getCurrentUser() {
		return this.userDirectoryService.getCurrentUser();
	}

	/**
	 * Get a property from sakai.properties
	 *
	 * @param propName name of property
	 * @param dflt default value if property not set
	 * @return
	 */
	public String getSakaiProperty(final String propName, final String dflt) {
		return this.serverConfigurationService.getString(propName, dflt);
	}

	/**
	 * Is the current user a super user?
	 *
	 * @return
	 */
	public boolean isSuperUser() {
		final User user = this.userDirectoryService.getCurrentUser();
		return this.securityService.isSuperUser(user.getId());
	}

	/**
	 * Get the user's preferred locale from the Sakai resource loader
	 *
	 * @return
	 */
	public Locale getUserPreferredLocale() {
		final ResourceLoader rl = new ResourceLoader();
		return rl.getLocale();
	}

	/**
	 * Gets a list of the tools that have been configured in sakai.properties and are added to the current site and are registered with the
	 * archiver.
	 *
	 * @return list of {@link ArchiveableTool}s. May be empty.
	 */
	public List<ArchiveableTool> getArchiveableTools() {

		final List<ArchiveableTool> tools = new ArrayList<>();

		final List<String> toolIds = new ArrayList<>(ArchiverRegistry.getInstance().getRegistry().keySet());
		toolIds.retainAll(Arrays.asList(this.serverConfigurationService.getStrings("archiver.tools")));

		if (!toolIds.isEmpty()) {

			final String siteId = getCurrentSiteId();
			final Set<ArchiveableTool> siteTools = getToolsForSite(siteId);

			log.debug("siteTools: " + siteTools);
			log.debug("toolIds: " + toolIds);

			// remove tools that are not in the list of configured tools
			siteTools.removeIf(tool -> !toolIds.contains(tool.getToolId()));
			tools.addAll(siteTools);
		}

		Collections.sort(tools);

		log.debug("Archiveable tools {}", tools);

		return tools;
	}

	/**
	 * Checks if an archive is running for the current site.
	 *
	 * @return true/false
	 */
	public boolean isArchiveInProgress() {
		final String siteId = getCurrentSiteId();

		final Archive archive = this.archiverService.getLatest(siteId);
		return (archive != null && archive.getStatus() == Status.STARTED);

	}

	/**
	 * Checks if an archive was recently completed for the current site. Recent is defined as last 30 seconds.
	 *
	 * @return
	 */
	public boolean isArchiveRecentlyCompleted() {
		final String siteId = getCurrentSiteId();
		final Archive archive = this.archiverService.getLatest(siteId);
		if (archive != null && archive.getStatus() == Status.COMPLETE) {
			// check if difference between now and the finish date is less than 2 minutes

			final LocalDateTime archiveEndDate = archive.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			final LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());

			final long difference = ChronoUnit.SECONDS.between(archiveEndDate, now);
			if (difference <= 120) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Start a new archive for the current site and initiated by the current user
	 *
	 * @param settings the settings for this archive, from the UI
	 *
	 * @throws {@link ToolsNotSpecifiedException} if no tools are specified
	 * @throws {@link ArchiveAlreadyInProgressException} if an archive is already in progress for the given site
	 * @throws {@link ArchiveInitialisationException} if the archive could not be initialised
	 * @throws {@link ArchiveCompletionException} if the archive could not be completed properly
	 */
	public void createArchive(final ArchiveSettings settings)
			throws ToolsNotSpecifiedException, ArchiveAlreadyInProgressException, ArchiveInitialisationException,
			ArchiveCompletionException {

		log.debug("settings: " + settings);

		final String siteId = getCurrentSiteId();
		final String userUuid = getCurrentUser().getId();

		// get tools to include in the archive
		final String[] toolIds = settings.getArchiveableTools().stream().filter(t -> t.isIncludeInArchive()).map(t -> t.getToolId())
				.toArray(String[]::new);

		this.archiverService.startArchive(siteId, userUuid, settings.isIncludeStudentData(), toolIds);
	}

	/**
	 * Get a list of Archives for the given site.
	 *
	 * @param siteId the siteId to get the archives for
	 * @param max the maximum number to return
	 * @return
	 */
	public List<Archive> getArchives(final String siteId, final int max) {
		return this.archiverService.getArchives(siteId, max);
	}

	/**
	 * Get a user's display name
	 *
	 * @param userUuid
	 * @return display name or uuid if user cannot be found
	 */
	public String getUserDisplayName(final String userUuid) {
		try {
			return this.userDirectoryService.getUser(userUuid).getDisplayName();
		} catch (final UserNotDefinedException e) {
			log.debug("No user found for {}", userUuid);
			return userUuid;
		}
	}

	/**
	 * Get the zip file for an archive of type {@link File}
	 *
	 * @param archive
	 * @return the file
	 * @throws {@link ArchiveNotFoundException} if the zipFile no longer exists
	 */
	public File getArchiveZip(final Archive archive) throws ArchiveNotFoundException {

		final String zipPath = archive.getZipPath();

		final File zipFile = new File(zipPath);
		if (!zipFile.exists() || !zipFile.isFile()) {
			log.error("Zip file does not exist on the filesystem: " + zipFile);
			throw new ArchiveNotFoundException();
		}
		return zipFile;
	}

	/**
	 * Find sites matching the search string
	 *
	 * @param search search string
	 * @return
	 */
	public List<ArchiveSite> findSites(final String search) {
		final List<Site> sites = this.siteService.getSites(SelectionType.NON_USER, null, search, null, SortType.TITLE_ASC, null);
		return sites.stream().map(s -> new ArchiveSite(s.getId(), s.getTitle())).collect(Collectors.toList());
	}

	/**
	 * Helper to get siteid. This will ONLY work in a portal site context, it will return null otherwise (ie via an entityprovider).
	 *
	 * @return
	 */
	public String getCurrentSiteId() {
		try {
			return this.toolManager.getCurrentPlacement().getContext();
		} catch (final Exception e) {
			return null;
		}
	}

	/**
	 * Get the title of a site
	 *
	 * @param siteId
	 * @return the site title or fallback to the siteid if the site can't be found
	 */
	public String getSiteTitle(final String siteId) {
		try {
			final Site site = this.siteService.getSite(siteId);
			return site.getTitle();
		} catch (final IdUnusedException e) {
			return siteId;
		}
	}

	/**
	 * Cancel an archive
	 * 
	 * @param archiveId the id of the archive to cancel
	 *
	 * @throws {@link ArchiveCancellationException} if the archive could not be cancelled
	 */
	public void cancelArchive(final String archiveId) throws ArchiveCancellationException {
		this.archiverService.cancelArchive(archiveId);
	}

	/**
	 * Get the list of tools in a site and use the the page title so it is familiar to the instructor
	 *
	 * @param siteId the site to check
	 * @return Set of {@link ArchiveableTool} which wraps toolId and title.
	 */
	private Set<ArchiveableTool> getToolsForSite(final String siteId) {

		final Set<ArchiveableTool> tools = new HashSet<>();

		Site site;
		try {
			site = this.siteService.getSite(siteId);
		} catch (final IdUnusedException e) {
			log.error("Invalid site. Cannot lookup tools");
			return tools;
		}

		for (final SitePage page : site.getPages()) {

			for (final ToolConfiguration toolConfig : page.getTools()) {
				tools.add(new ArchiveableTool(toolConfig.getToolId(), page.getTitle()));
			}
		}

		return tools;
	}

}
