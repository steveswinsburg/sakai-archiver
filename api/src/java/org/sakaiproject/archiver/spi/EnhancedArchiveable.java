package org.sakaiproject.archiver.spi;

import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;

/**
 * Services which implement {@link EnhancedArchiveable} declare that they are able to archive themselves.
 *
 * Any service implementing this must also register themselves with the {@link ArchiverRegistry}.
 *
 * To actually create the aggregated archive, you must use the methods in {@link ArchiverService}.
 *
 * @since 12.0
 * @author Steve Swinsburg (steve.swinsburg@gmail.com)
 */
public interface EnhancedArchiveable extends Archiveable {

	/**
	 * The toolId that this archiver should be registered under.
	 *
	 * Must not be blank and must be unique of your service will not be registered.
	 *
	 * @return
	 */
	String getToolId();

	/**
	 * If this archiver is to have it's data linked into another tool's archive, specify the toolId of that archiver.
	 *
	 * @return
	 */
	String getLinkedToolId();

	/**
	 * The human readable name to be used for the directory within the archive
	 *
	 * @return
	 */
	String getName();

}
