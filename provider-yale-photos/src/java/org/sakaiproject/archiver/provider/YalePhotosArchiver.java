package org.sakaiproject.archiver.provider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
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
import org.sakaiproject.archiver.provider.model.RosterEntry;
import org.sakaiproject.archiver.spi.Archiveable;
import org.sakaiproject.archiver.util.Htmlifier;
import org.sakaiproject.entitybroker.EntityBroker;
import org.sakaiproject.entitybroker.entityprovider.extension.ActionReturn;
import org.sakaiproject.entitybroker.exception.EntityNotFoundException;
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

	private static final String IMAGES_SUBDIR = "images";

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

		final String toolName = getToolName(siteId);

		// export the photos
		final List<String> studentUuids = getStudentUuids(siteId);
		final List<User> users = getUsers(studentUuids);
		final List<String> studentEids = getStudentEids(users);

		// refresh photo cache
		try {
			this.photoService.loadPhotos(studentEids, siteId);
		} catch (final YalePhotoDirectoryServiceException e) {
			log.error("Error refreshing the photo cache, the export may be incomplete", e);
		}

		// get photo for each student
		// RosterTool hardcoded to JPEG so we do the same here
		studentEids.forEach(eid -> {
			try {
				final byte[] photoBytes = this.photoService.loadPhotoFromCache(eid);
				this.archiverService.archiveContent(archiveId, siteId, toolName, photoBytes, getImageFilename(eid), IMAGES_SUBDIR);
			} catch (final YalePhotoDirectoryServiceException e) {
				log.error("Error getting photo for {}, the export will be incomplete", e);
			}
		});

		// get data for XLSX export
		final byte[] rosterExport = getRosterExport(siteId);
		final ByteArrayInputStream is = new ByteArrayInputStream(rosterExport);

		try {
			final Workbook wb = WorkbookFactory.create(is);
			final Sheet sheet = wb.getSheetAt(0);

			final Map<Integer, String> header = new LinkedHashMap<>();
			final List<Map<Integer, String>> contents = new ArrayList<>();

			for (final Row row : sheet) {
				if (row.getRowNum() == 2) {
					header.putAll(processRow(row));
				}
				if (row.getRowNum() >= 4) {
					// process data
					final Map<Integer, String> data = processRow(row);
					contents.add(data);
				}
			}

			log.debug("header: " + header);
			log.debug("contents: " + contents);

			if (header.isEmpty()) {
				throw new InvalidFormatException("Spreadsheet did not have a header");
			}
			if (contents.isEmpty()) {
				throw new InvalidFormatException("Spreadsheet did not have any data");
			}

			// process data into list of roster entries
			// for every row, link up the data with the header and create a list of roster entries
			final List<RosterEntry> entries = new ArrayList<>();
			contents.forEach(data -> {

				final RosterEntry r = new RosterEntry();

				header.forEach((k, v) -> {

					// pick apart each row. Numbers are the column indexes in the spreadsheet data, 0 based.
					switch (k) {

						case 0:
							final String displayName = data.get(k);
							r.setDisplayName(displayName);
							break;
						case 1:
							final String eid = data.get(k);
							r.setEid(eid);
							r.setPhotoUrl(IMAGES_SUBDIR + File.separatorChar + getImageFilename(eid));
							break;
						case 2:
							final String email = data.get(k);
							r.setEmailAddress(email);
							break;
						case 3:
							final String role = data.get(k);
							r.setRole(role);
							break;
						default:
							final String value = data.get(k);
							if (StringUtils.isNotBlank(value)) {
								r.addField(v, data.get(k));
							}
					}

				});

				entries.add(r);
			});

			log.debug("roster entries: " + entries);

			// now turn the list into some nice HTML
			final String htmlBody = getAsHtml(entries);
			final String html = Htmlifier.toHtml(htmlBody, this.archiverService.getSiteHeader(siteId, TOOL_ID));
			log.debug("html: " + html);

			this.archiverService.archiveContent(archiveId, siteId, toolName, html.getBytes(), "roster.html");

		} catch (InvalidFormatException | EncryptedDocumentException | IOException e) {
			log.error("Roster export could not be processed", e);
			return;
		}

	}

	@Override
	public String getToolName(final String siteId) {
		return this.archiverService.getToolName(siteId, TOOL_ID);
	}

	/**
	 * Process a row to extract column names and their positions
	 *
	 * @param row the raw row data
	 * @return map of column number to data in column
	 */
	private Map<Integer, String> processRow(final Row row) {
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
	 * Standardise the formatting of the filename for an image
	 *
	 * @param eid
	 * @return
	 */
	private String getImageFilename(final String eid) {
		return eid + ".jpg";
	}

	/**
	 * Render a list of roster entries as HTML
	 *
	 * This uses bootstrap classes and one row of many columns which will auto wrap. For more control over the layout it could be split into
	 * multiple rows.
	 *
	 * @param entries
	 * @return
	 */
	private String getAsHtml(final List<RosterEntry> entries) {

		final StringBuilder sb = new StringBuilder();

		// get the count of each role in the list
		final Map<String, Long> roleCount = entries.stream().collect(Collectors.groupingBy(e -> e.getRole(), Collectors.counting()));

		// add a role tally
		sb.append("<div class=\"lead\">");
		roleCount.forEach((k, v) -> {
			sb.append(k + "&nbsp;<strong>" + v + "</strong><br />");
		});
		sb.append("</div>");

		// output the roster data
		sb.append("<div class=\"row\">");

		entries.forEach(entry -> {
			sb.append("<div class=\"col-xs-3\">");
			sb.append("<img class=\"img-rounded\" src=\"" + entry.getPhotoUrl() + "\">");
			sb.append("<div>" + entry.getDisplayName() + "</div>");
			sb.append("<div>Role: " + entry.getRole() + "</div>");
			sb.append("<div>" + entry.getEmailAddress() + "</div>");

			entry.getFields().forEach((field, value) -> {
				sb.append("<div>" + field + ": " + value + "</div>");
			});
			sb.append("<br />"); // spacer hack
			sb.append("</div>");
		});

		sb.append("</div>");

		return sb.toString();

	}

}
