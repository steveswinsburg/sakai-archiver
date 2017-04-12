package org.sakaiproject.archiver.business;

import java.util.Locale;

import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.site.api.SiteService;
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

}
