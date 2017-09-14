package org.sakaiproject.archiver.provider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.spi.Archiveable;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link Archiveable} for the Resources tool
 *
 * @author Steve Swinsburg(steve.swinsburg@gmail.com)
 * @since 12.0
 */
@Slf4j
public class ResourcesArchiver implements Archiveable {

	private static final String TOOL_ID = "sakai.resources";

	public void init() {
		ArchiverRegistry.getInstance().register(TOOL_ID, this);
	}

	public void destroy() {
		ArchiverRegistry.getInstance().unregister(TOOL_ID);
	}

	@Setter
	private SiteService siteService;

	@Setter
	private ContentHostingService contentHostingService;

	@Setter
	private UserDirectoryService userDirectoryService;

	@Setter
	private ArchiverService archiverService;

	@Override
	public void archive(final String archiveId, final String siteId, final boolean includeStudentContent) {

		final String collectionId = getSiteCollectionId(siteId);
		final List<ContentResource> resources = this.contentHostingService.getAllResources(collectionId);

		final String toolName = getToolName(siteId);

		// TODO do we care about controlling student uploaded files here?
		// List<String> studentUuids = this.getStudentUuids(siteId);

		// maintain a cache of display names for this archive run
		final Map<String, String> userDisplayNames = new HashMap<>();

		resources.forEach(resource -> {

			final String creatorUuid = getCreatorUuid(resource);
			userDisplayNames.computeIfAbsent(creatorUuid, k -> getUserDisplayName(creatorUuid));

			final String[] subdirs = getSubDirs(siteId, resource);
			final String filename = getFilename(resource);

			log.debug("resource: {}", resource.getUrl());

			try {
				this.archiverService.archiveContent(archiveId, siteId, toolName, resource.getContent(), filename, subdirs);
			} catch (final ServerOverloadException e) {
				log.error("Error retrieving data for resource {}", resource.getUrl(true));
			}

		});
	}

	@Override
	public String getToolName(final String siteId) {
		return this.archiverService.getToolName(siteId, TOOL_ID);
	}

	/**
	 * Get the collection Id for the site
	 *
	 * @param siteId
	 * @return
	 */
	private String getSiteCollectionId(final String siteId) {
		return ContentHostingService.COLLECTION_SITE + siteId + "/";
	}

	/**
	 * Get the hierarchy of subdirectories that this resource is contained within
	 *
	 * @param siteId
	 * @param collectionUrl
	 * @return String[] or null
	 */
	private String[] getSubDirs(final String siteId, final ContentResource resource) {

		// get the parent collection and remove the /content/group/{siteid}/userId prefix
		// anything remaining is turned into subdirectories
		final String containingCollectionId = resource.getContainingCollection().getUrl(true);
		final String prefix = "/content" + getSiteCollectionId(siteId);
		final String path = StringUtils.removeStart(containingCollectionId, prefix);

		return StringUtils.split(path, "/");
	}

	/**
	 * Get the name of the file
	 *
	 * @param resource
	 * @return
	 */
	private String getFilename(final ContentResource resource) {
		final ResourceProperties props = resource.getProperties();
		return props.getProperty(ResourceProperties.PROP_DISPLAY_NAME);
	}

	/**
	 * Get the creator uuid of the file
	 *
	 * @param resource
	 * @return
	 */
	private String getCreatorUuid(final ContentResource resource) {
		final ResourceProperties props = resource.getProperties();
		return props.getProperty(ResourceProperties.PROP_CREATOR);
	}

	/**
	 * Get the display name for a single user. Fall back to uuid if not found
	 *
	 * @param userUuid uuid to lookup
	 * @return
	 */
	private String getUserDisplayName(final String userUuid) {
		try {
			return this.userDirectoryService.getUser(userUuid).getDisplayName();
		} catch (final UserNotDefinedException e) {
			log.debug("User {} could not be found, falling back to uuid", userUuid);
		}
		return userUuid;
	}

}
