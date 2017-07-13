package org.sakaiproject.archiver.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.spi.Archiveable;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;

import edu.yale.its.amt.sakai.directory.YalePhotoDirectoryService;
import edu.yale.its.amt.sakai.directory.YalePhotoDirectoryServiceException;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link Archiveable} for Yales Photo Roster tool
 *
 * Whilst this is based on the Roster2 tool the customisations for the photos means that this needs to be handled separately. Since there is
 * alrady a Roster2 tool archiver, this is a custom registration.
 *
 * @author Steve Swinsburg(steve.swinsburg@gmail.com)
 * @since 12.0
 */
@Slf4j
public class YalePhotosArchiver implements Archiveable {

	// register this with the same ID as the
	private static final String TOOL_ID = "sakai.site.roster2";
	private static final String TOOL_NAME = "Roster";

	public void init() {
		ArchiverRegistry.getInstance().register(TOOL_ID, this);
	}

	public void destroy() {
		ArchiverRegistry.getInstance().unregister(TOOL_ID);
	}

	@Setter
	private SiteService siteService;

	@Setter
	private YalePhotoDirectoryService photoService;

	@Setter
	private UserDirectoryService userDirectoryService;

	@Setter
	private ArchiverService archiverService;

	@Override
	public void archive(final String archiveId, final String siteId, final boolean includeStudentContent) {

		if (!includeStudentContent) {
			log.warn("Student photos cannot be archived without student content enabled");
			return;
		}

		final List<String> studentUuids = getStudentUuids(siteId);
		final List<User> users = getUsers(studentUuids);
		final List<String> studentEids = getStudentEids(users);

		final Map<String, String> studentNames = getUserDisplayNames(users);

		// refresh photo cache
		try {
			this.photoService.loadPhotos(studentEids);
		} catch (final YalePhotoDirectoryServiceException e) {
			log.error("Error refreshing the photo cache, the export may be incomplete", e);
		}

		// get photo for each student
		// RosterTool hardcoded to JPEG so we do the same here
		studentEids.forEach(eid -> {
			try {
				final byte[] photo = this.photoService.loadPhotoFromCache(eid);
				final String filename = getStudentName(eid, studentNames) + ".jpg";
				this.archiverService.archiveContent(archiveId, siteId, TOOL_NAME, photo, filename);
			} catch (final YalePhotoDirectoryServiceException e) {
				log.error("Error getting photo for {}, the export will be incomplete", e);
			}

		});

	}

	/**
	 * Get a list of students in the site, as uuids
	 *
	 * @param siteId
	 * @return list of uuids
	 */
	private List<String> getStudentUuids(final String siteId) {
		try {
			final Set<String> userIds = this.siteService.getSite(siteId).getUsersIsAllowed("site.visit");
			return new ArrayList<>(userIds);
		} catch (final IdUnusedException e) {
			log.error("No users in site: {}", siteId);
			return Collections.emptyList();
		}
	}

	/**
	 * Get a list of {@link User}s
	 *
	 * @param uuids list of uuids to get Users for
	 * @return list of {@link User}
	 */
	private List<User> getUsers(final List<String> uuids) {
		return this.userDirectoryService.getUsers(uuids);
	}

	/**
	 * Get a list of eids
	 *
	 * @param users the list of {@link User}s to get the data for
	 * @return List of eids
	 */
	private List<String> getStudentEids(final List<User> users) {
		final List<String> eidList = users.stream().map(u -> u.getEid()).collect(Collectors.toList());
		return eidList;
	}

	/**
	 * Get map of eid to display name
	 *
	 * @param users the list of {@link User}s to get the data for
	 * @return Map of eid to display name
	 */
	private Map<String, String> getUserDisplayNames(final List<User> users) {
		final Map<String, String> userMap = users.stream().collect(Collectors.toMap(User::getEid, User::getSortName));
		return userMap;
	}

	/**
	 * Get a name from the map, falling back to eid if no name found
	 *
	 * @param studentEid the eid to lookup to map with
	 * @param studentNames the map of uuid to display name
	 * @return
	 */
	private final String getStudentName(final String studentEid, final Map<String, String> studentNames) {
		String studentName = studentNames.get(studentEid);
		if (StringUtils.isBlank(studentName)) {
			studentName = studentEid;
		}
		return studentName;
	}

}
