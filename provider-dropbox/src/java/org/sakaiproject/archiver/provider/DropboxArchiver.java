package org.sakaiproject.archiver.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.spi.Archiveable;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link Archiveable} for the Dropbox tool
 *
 * @author Steve Swinsburg(steve.swinsburg@gmail.com)
 * @since 12.0
 */
@Slf4j
public class DropboxArchiver implements Archiveable {

	private static final String TOOL_ID = "sakai.dropbox";

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

		if (!includeStudentContent) {
			log.warn("Dropbox cannot be archived without student content enabled");
			return;
		}
		final List<String> studentUuids = getStudentUuids(siteId);
		final Map<String, String> studentNames = getUserDisplayNames(studentUuids);

		studentUuids.forEach(studentUuid -> {
			final String collectionId = getDropBoxCollectionId(siteId, studentUuid);
			final List<ContentResource> resources = this.contentHostingService.getAllResources(collectionId);

			final String studentName = getStudentName(studentUuid, studentNames);

			resources.forEach(resource -> {

				final String[] subdirs = prepend(studentName, getSubDirs(siteId, studentUuid, resource));

				try {
					this.archiverService.archiveContent(archiveId, siteId, TOOL_ID, resource.getContent(), getFilename(resource), subdirs);
				} catch (final ServerOverloadException e) {
					log.error("Error retrieving data for resource {}", resource.getUrl(true));
				}

			});

		});

		//
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
	 * Get the collection Id for the dropbox
	 *
	 * @param siteId
	 * @param userId
	 * @return
	 */
	private String getDropBoxCollectionId(final String siteId, final String userId) {
		return ContentHostingService.COLLECTION_DROPBOX + siteId + "/" + userId + "/";
	}

	/**
	 * Get the hierarchy of subdirectories that this resource is contained within
	 *
	 * @param siteId
	 * @param userId
	 * @param collectionUrl
	 * @return String[] or null
	 */
	private String[] getSubDirs(final String siteId, final String userId, final ContentResource resource) {

		// get the parent collection and remove the /content/group-user/{siteid}/userId prefix
		// anything remaining is turned into subdirectories
		final String containingCollectionId = resource.getContainingCollection().getUrl(true);
		final String prefix = "/content" + getDropBoxCollectionId(siteId, userId);
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
	 * Prepend a String to the beginning of an existing array
	 *
	 * @param element the string to add
	 * @param array the array to add it to
	 * @return
	 */
	private String[] prepend(final String element, final String[] array) {
		return (String[]) ArrayUtils.add(array, 0, element);
	}

	/**
	 * Get map of userId to display name
	 *
	 * @param userUuids the list of uuids to lookup
	 * @return
	 */
	private Map<String, String> getUserDisplayNames(final List<String> userUuids) {
		final List<User> users = this.userDirectoryService.getUsers(userUuids);
		final Map<String, String> userMap = users.stream().collect(Collectors.toMap(User::getId, User::getSortName));
		return userMap;
	}

	/**
	 * Get a name from the map, falling back to uuid if no name found
	 *
	 * @param studentUuid the uuid to lookup
	 * @param studentNames the map of uuid to display name
	 * @return
	 */
	private final String getStudentName(final String studentUuid, final Map<String, String> studentNames) {
		String studentName = studentNames.get(studentUuid);
		if (StringUtils.isBlank(studentName)) {
			studentName = studentUuid;
		}
		return studentName;
	}

}
