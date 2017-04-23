package org.sakaiproject.archiver.app.business;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.app.model.ArchiveableTool;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.Tool;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
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
	 * Get the user's preferred locale from the Sakai resource loader
	 *
	 * @return
	 */
	public Locale getUserPreferredLocale() {
		final ResourceLoader rl = new ResourceLoader();
		return rl.getLocale();
	}

	/**
	 * Gets a list of the tools that have been configured in sakai.properties
	 *
	 * @return list of {@link ArchiveableTool}s. May be empty.
	 */
	public List<ArchiveableTool> getConfiguredTools() {
		final List<ArchiveableTool> tools = new ArrayList<>();

		final String[] toolIds = this.serverConfigurationService.getStrings("archiver.tools");
		if (toolIds != null) {
			Arrays.asList(toolIds).forEach(toolId -> {
				final Tool t = this.toolManager.getTool(toolId);

				final ArchiveableTool tool = new ArchiveableTool(toolId, t.getTitle());
				tools.add(tool);
			});
		}

		return tools;
	}
	
	/**
	 * Checks if an archive is running for the current site.
	 * Wrapper for {@link ArchiverService#isArchiveInProgress(String)}
	 * @return true/false
	 */
	public boolean isArchiveInProgress() {
		final String siteId = getCurrentSiteId();
		return archiverService.isArchiveInProgress(siteId);
	}
	
	/**
	 * Helper to get siteid. This will ONLY work in a portal site context, it will return null otherwise (ie via an entityprovider).
	 *
	 * @return
	 */
	private String getCurrentSiteId() {
		try {
			return this.toolManager.getCurrentPlacement().getContext();
		} catch (final Exception e) {
			return null;
		}
	}

}
