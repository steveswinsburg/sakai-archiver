package org.sakaiproject.archiver.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.spi.Archiveable;
import org.sakaiproject.archiver.util.Jsonifier;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

import lombok.Getter;
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
	public void archive(final String archiveId, final String siteId, final String toolId, final boolean includeStudentContent) {

		final String collectionId = getSiteCollectionId(siteId);
		final List<ContentResource> resources = this.contentHostingService.getAllResources(collectionId);

		// TODO do we care about student uploaded files here?
		// List<String> studentUuids = this.getStudentUuids(siteId);

		final List<Metadata> metadata = new ArrayList<>();

		// maintain a cache of display names for this archive run
		final Map<String, String> userDisplayNames = new HashMap<>();

		resources.forEach(resource -> {

			final String creatorUuid = getCreatorUuid(resource);
			userDisplayNames.computeIfAbsent(creatorUuid, k -> getUserDisplayName(creatorUuid));

			final String[] subdirs = getSubDirs(siteId, resource);
			final String filename = getFilename(resource);

			log.debug("resource: {}", resource.getUrl());

			try {
				this.archiverService.archiveContent(archiveId, siteId, TOOL_ID, resource.getContent(), filename, subdirs);
				metadata.add(createMetadata(resource, subdirs, filename, userDisplayNames.get(creatorUuid)));
			} catch (final ServerOverloadException e) {
				log.error("Error retrieving data for resource {}", resource.getUrl(true));
			}

		});

		final String json = Jsonifier.toJson(metadata);
		log.debug("Resources JSON: {} ", json);

		this.archiverService.archiveContent(archiveId, siteId, TOOL_ID, json.getBytes(), "index.json");

	}

	/**
	 * Get a list of students in the site, as uuids
	 *
	 * @param siteId
	 * @return
	 */
	private List<String> getStudentUuids(final String siteId) {
		try {
			final Set<String> userIds = this.siteService.getSite(siteId).getUsersIsAllowed("dropbox.own");
			return new ArrayList<>(userIds);
		} catch (final IdUnusedException e) {
			log.error("No users in site: {}", siteId);
			return Collections.emptyList();
		}
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
	 * Get the creation date of the file
	 *
	 * @param resource
	 * @return
	 */
	private String getCreationDate(final ContentResource resource) {
		final ResourceProperties props = resource.getProperties();
		return props.getProperty(ResourceProperties.PROP_CREATION_DATE);
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

	/**
	 * Create a Metadata object for this resource and other data
	 *
	 * @param resource
	 * @param subdirs
	 * @param filename
	 * @param creatorDisplayName
	 * @return
	 */
	private Metadata createMetadata(final ContentResource resource, final String[] subdirs, final String filename,
			final String creatorDisplayName) {
		final Metadata m = new Metadata();
		m.setDir(String.join("/", subdirs));
		m.setFilename(filename);
		m.setCreator(creatorDisplayName);
		m.setCreated(getCreationDate(resource));
		return m;
	}

	/**
	 * Metadata for a resource in the site
	 */
	public static class Metadata {

		@Getter
		@Setter
		private String dir;

		@Getter
		@Setter
		private String filename;

		@Getter
		@Setter
		private String creator;

		@Getter
		@Setter
		private String created;

	}

}
