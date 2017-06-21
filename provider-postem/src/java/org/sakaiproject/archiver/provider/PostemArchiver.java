package org.sakaiproject.archiver.provider;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.sakaiproject.api.app.postem.data.Gradebook;
import org.sakaiproject.api.app.postem.data.GradebookManager;
import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.spi.Archiveable;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.exception.TypeException;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link Archiveable} for the Postem tool
 *
 * @author Steve Swinsburg(steve.swinsburg@gmail.com)
 * @since 12.0
 */
@Slf4j
public class PostemArchiver implements Archiveable {

	private static final String TOOL_ID = "sakai.postem";

	public void init() {
		ArchiverRegistry.getInstance().register(TOOL_ID, this);
	}

	public void destroy() {
		ArchiverRegistry.getInstance().unregister(TOOL_ID);
	}

	@Setter
	private ContentHostingService contentHostingService;

	@Setter
	private EntityManager entityManager;

	@Setter
	private GradebookManager postemGradebookManager;

	@Setter
	private ArchiverService archiverService;

	@SuppressWarnings("unchecked")
	@Override
	public void archive(final String archiveId, final String siteId, final String toolId, final boolean includeStudentContent) {

		final List<Gradebook> gradebooks = new ArrayList<>(
				this.postemGradebookManager.getGradebooksByContext(siteId, Gradebook.SORT_BY_TITLE, true));

		gradebooks.forEach(gradebook -> {
			final byte[] fileContents = getFileContents(gradebook);
			final String filename = getFileName(gradebook);

			if (ArrayUtils.isNotEmpty(fileContents)) {
				this.archiverService.archiveContent(archiveId, siteId, toolId, fileContents, filename);
			}

		});

	}

	/**
	 * Get the contents of the file that was uploaded for this postem gradebook entry
	 *
	 * @param gradebook the Postem {@link Gradebook}
	 * @return
	 */
	private byte[] getFileContents(final Gradebook gradebook) {

		byte[] contents = null;

		final Reference reference = this.entityManager.newReference(this.contentHostingService.getReference(gradebook.getFileReference()));

		try {
			final ContentResource resource = this.contentHostingService.getResource(reference.getId());
			contents = resource.getContent();
		} catch (PermissionException | IdUnusedException | TypeException | ServerOverloadException e) {
			log.debug("Error retrieving postem gradebook '{}' in site {}", gradebook.getTitle(), gradebook.getContext(), e);
		}
		return contents;
	}

	/**
	 * Get the filename for a download. Matches postem tool format.
	 *
	 * @param gradebook the Postem {@link Gradebook}
	 * @return
	 */
	private String getFileName(final Gradebook gradebook) {
		final StringBuilder sb = new StringBuilder();
		sb.append("postem_");
		sb.append(gradebook.getTitle());
		sb.append(".csv");
		return sb.toString();
	}

}
