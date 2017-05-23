package org.sakaiproject.archiver.download;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.api.Status;
import org.sakaiproject.archiver.dto.Archive;
import org.sakaiproject.archiver.exception.ArchiveNotFoundException;
import org.sakaiproject.archiver.exception.ArchivePermissionException;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.api.ComponentManager;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.SessionManager;

import lombok.extern.slf4j.Slf4j;

/**
 * Download servlet for archiver downloads. Must be a maintainer or super user of the site the archive is for.
 *
 * @author Steve Swinsburg (steve.swinsburg@gmail.com)
 * @since 12.0
 */
@Slf4j
public class DownloadServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private ArchiverService archiverService;
	private SessionManager sessionManager;
	private SecurityService securityService;
	private SiteService siteService;

	public DownloadServlet() {
		super();

		// must use the ComponentManager for Servlet dependency injection
		final ComponentManager manager = org.sakaiproject.component.cover.ComponentManager.getInstance();

		if (this.archiverService == null) {
			this.archiverService = (ArchiverService) manager.get(ArchiverService.class.getName());
		}
		if (this.sessionManager == null) {
			this.sessionManager = (SessionManager) manager.get(SessionManager.class.getName());
		}
		if (this.securityService == null) {
			this.securityService = (SecurityService) manager.get(SecurityService.class.getName());
		}
		if (this.siteService == null) {
			this.siteService = (SiteService) manager.get(SiteService.class.getName());
		}

	}

	@Override
	public void init(final ServletConfig config) throws ServletException {

	}

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		final String currentUserId = getCurrentUserId();
		if (StringUtils.isBlank(currentUserId)) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "You must be logged in to download archives");
			return;
		}

		final String archiveId = request.getParameter("archiveId");
		if (StringUtils.isBlank(archiveId)) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "You must supply an archive id");
			return;
		}

		Archive archive = null;
		try {
			archive = this.archiverService.getArchive(archiveId);
		} catch (final ArchiveNotFoundException e) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
			return;
		}

		if (archive.getStatus() != Status.COMPLETE) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Archive is not yet complete and cannot be downloaded at this time");
			return;
		}

		final String siteId = archive.getSiteId();
		try {
			checkPermission(currentUserId, siteId);
		} catch (final IdUnusedException e) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
			return;
		} catch (final ArchivePermissionException e) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
			return;
		}

		final String zipPath = archive.getZipPath();
		if (StringUtils.isBlank(zipPath)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Archive path is incomplete and cannot be downloaded at this time");
			return;
		}

		// output the file
		final String fileName = FilenameUtils.getName(zipPath);
		response.setContentType("application/zip");
		response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
		final OutputStream out = response.getOutputStream();

		try (InputStream in = FileUtils.openInputStream(new File(zipPath))) {
			IOUtils.copyLarge(in, out);
			out.flush();
			out.close();
		}

	}

	/**
	 * Helper to get current user id
	 *
	 * @return
	 */
	private String getCurrentUserId() {
		return this.sessionManager.getCurrentSessionUserId();
	}

	/**
	 * Verify this user can download archives in this site
	 *
	 * @param userId uuid of the user to check
	 * @param siteId site id to check the user's role in
	 *
	 * @throws {@link IdUnusedException} if site cannot be found
	 * @throws {@link ArchivePermissionException} if user doesn't have the right permissions
	 */
	private void checkPermission(final String userId, final String siteId) throws IdUnusedException, ArchivePermissionException {
		final String siteRef = this.siteService.getSite(siteId).getReference();
		if (!this.securityService.unlock(userId, SiteService.SECURE_UPDATE_SITE, siteRef)) {
			throw new ArchivePermissionException("User " + userId + " does not have the required permissions in site " + siteId);
		}
	}

}
