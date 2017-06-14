package org.sakaiproject.archiver.provider;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.apache.commons.lang.ArrayUtils;
import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.spi.Archiveable;
import org.sakaiproject.entitybroker.EntityBroker;
import org.sakaiproject.entitybroker.entityprovider.extension.ActionReturn;
import org.sakaiproject.entitybroker.exception.EntityNotFoundException;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link Archiveable} for the Roster2 tool
 *
 * @author Steve Swinsburg(steve.swinsburg@gmail.com)
 * @since 12.0
 */
@Slf4j
public class Roster2Archiver implements Archiveable {

	private static final String TOOL_ID = "sakai.site.roster2";

	public void init() {
		ArchiverRegistry.getInstance().register(TOOL_ID, this);
	}

	public void destroy() {
		ArchiverRegistry.getInstance().unregister(TOOL_ID);
	}

	@Setter
	private EntityBroker entityBroker;

	@Setter
	private ArchiverService archiverService;

	@Override
	public void archive(final String archiveId, final String siteId, final String toolId, final boolean includeStudentContent) {
		final byte[] rosterExport = getRosterExport(siteId);

		if (ArrayUtils.isNotEmpty(rosterExport)) {
			this.archiverService.archiveContent(archiveId, siteId, TOOL_ID, rosterExport, "roster-export.xlsx");
		}

	}

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

}
