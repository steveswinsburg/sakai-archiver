package org.sakaiproject.archiver.provider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.spi.Archiveable;
import org.sakaiproject.entitybroker.EntityBroker;
import org.sakaiproject.entitybroker.entityprovider.extension.ActionReturn;
import org.sakaiproject.entitybroker.exception.EntityNotFoundException;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;

import edu.yale.its.amt.sakai.directory.YalePhotoDirectoryService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link Archiveable} for Yales Photo Roster tool
 *
 * Whilst this is based on the Roster2 tool, the Yale specific customisations for the photos means that this needs to be archived
 * separately.
 *
 * This performs some of the same logic as the Roster2 archiver (getting the Excel export) but then parses it to build a HTML file for the
 * students in the class. It does not write out the Excel file into the archive (though it could, same as Roster2).
 *
 * @author Steve Swinsburg(steve.swinsburg@gmail.com)
 * @since 12.0
 */
@Slf4j
public class YalePhotosArchiver implements Archiveable {

	// register this with the same ID as the Roster
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
	private EntityBroker entityBroker;

	@Setter
	private ArchiverService archiverService;

	@Override
	public void archive(final String archiveId, final String siteId, final boolean includeStudentContent) {

		if (!includeStudentContent) {
			log.warn("Student photos cannot be archived without student content enabled");
			return;
		}

		// get data for XLSX export
		final byte[] rosterExport = getRosterExport(siteId);
		final ByteArrayInputStream is = new ByteArrayInputStream(rosterExport);

		try {
			final Workbook wb = WorkbookFactory.create(is);
			final Sheet sheet = wb.getSheetAt(0);
			// final ArrayList<String> column = new ArrayList<>();

			Map<Integer, String> header = null;

			for (final Row row : sheet) {
				if (row.getRowNum() == 2) {
					header = processHeader(row);
				}
				if (row.getRowNum() >= 4) {
					// process data
				}

			}

		} catch (InvalidFormatException | EncryptedDocumentException | IOException e) {
			log.error("Roster export could not be processed", e);
			return;
		}

		/*
		 * final List<String> studentUuids = getStudentUuids(siteId); final List<User> users = getUsers(studentUuids); final List<String>
		 * studentEids = getStudentEids(users);
		 *
		 * final Map<String, String> studentNames = getUserDisplayNames(users);
		 *
		 * // refresh photo cache try { this.photoService.loadPhotos(studentEids); } catch (final YalePhotoDirectoryServiceException e) {
		 * log.error("Error refreshing the photo cache, the export may be incomplete", e); }
		 *
		 * // get photo for each student // RosterTool hardcoded to JPEG so we do the same here studentEids.forEach(eid -> { try { final
		 * byte[] photo = this.photoService.loadPhotoFromCache(eid); final String filename = getStudentName(eid, studentNames) + ".jpg";
		 * this.archiverService.archiveContent(archiveId, siteId, TOOL_NAME, photo, filename); } catch (final
		 * YalePhotoDirectoryServiceException e) { log.error("Error getting photo for {}, the export will be incomplete", e); }
		 *
		 * });
		 */

	}

	/**
	 * Process the header row
	 *
	 * @param row the raw row data
	 * @return map of column number to data in column
	 */
	private Map<Integer, String> processHeader(final Row row) {
		final Map<Integer, String> map = new LinkedHashMap<>();
		final DataFormatter cellFormatter = new DataFormatter();

		for (final Cell cell : row) {
			final String data = cellFormatter.formatCellValue(cell);
			map.put(cell.getColumnIndex(), data);
		}
		return map;
	}

	/**
	 * Identical to Roster2. Gets the export data for the Excel (.xslx) file.
	 *
	 * @param siteId
	 * @return
	 */
	private byte[] getRosterExport(final String siteId) {

		try {
			final ActionReturn ret = this.entityBroker.executeCustomAction("/roster-export/" + siteId, "get-export", null, null);

			final OutputStream out = ret.getOutput();
			if (out == null) {
				return null;
			}
			final ByteArrayOutputStream baos = (ByteArrayOutputStream) ret.getOutput();

			return baos.toByteArray();

		} catch (final EntityNotFoundException e) {
			log.error("Could not retrieve roster export", e);
		}
		return null;
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
