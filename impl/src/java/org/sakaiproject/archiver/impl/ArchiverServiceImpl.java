package org.sakaiproject.archiver.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.apache.commons.io.FileUtils;
import org.sakaiproject.archiver.api.Archiveable;
import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.dto.Archive;
import org.sakaiproject.archiver.exception.ArchiveAlreadyInProgressException;
import org.sakaiproject.archiver.exception.ArchiveInitialisationException;
import org.sakaiproject.archiver.exception.ArchiveWriterException;
import org.sakaiproject.archiver.exception.ToolsNotSpecifiedException;
import org.sakaiproject.archiver.persistence.ArchiverPersistenceService;
import org.sakaiproject.component.api.ServerConfigurationService;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ArchiverServiceImpl implements ArchiverService {

	@Setter
	private ArchiverPersistenceService dao;

	@Setter
	private ServerConfigurationService serverConfigurationService;

	public void init() {
		log.info("ArchiverService started");
	}

	@Override
	public boolean isArchiveInProgress(final String siteId) {

		final Archive archive = this.dao.getCurrent(siteId);
		return (archive != null) ? true : false;
	}

	@Override
	public void startArchive(final String siteId, final String userUuid, final boolean includeStudentData, final String... toolIds)
			throws ToolsNotSpecifiedException, ArchiveAlreadyInProgressException, ArchiveInitialisationException {

		// validate
		final List<String> toolsToArchive = Arrays.asList(toolIds);

		if (toolsToArchive.isEmpty()) {
			throw new ToolsNotSpecifiedException("Tools not specified");
		}

		if (isArchiveInProgress(siteId)) {
			throw new ArchiveAlreadyInProgressException("An archive is already in progress for this site");
		}

		// create the record
		final Archive archive = this.dao.create(siteId, userUuid);

		// create disk location
		try {
			FileUtils.forceMkdir(FileUtils.getFile(getArchiveBasePath()));
		} catch (final IOException e) {
			throw new ArchiveInitialisationException("Archive could not be started", e);
		}

		// now archive all of the registered tools
		// TODO this must run in a separate thread
		final Map<String, Archiveable> registry = ArchiverRegistry.getInstance().getRegistry();

		for (final String toolId : toolsToArchive) {
			final Archiveable archivable = registry.get(toolId);
			if (archivable == null) {
				log.error("No registered archiver for %S", toolId);
				break;
			}

			archivable.archive(archive.getArchiveId(), includeStudentData);
		}

	}

	@Override
	public void archiveContent(final String archiveId, final String toolId, final byte[] content, final String filename) {

		log.info("Archiving to %S for %S as %S with content of: %S", archiveId, toolId, filename, content.toString());

	}

	/**
	 * Get base dir for all archives as configured in sakai.properties
	 *
	 * @return
	 */
	private String getArchiveBasePath() {
		return this.serverConfigurationService.getString("archiver.path", FileUtils.getTempDirectoryPath());
	}

	@Override
	public void archiveContent(final String archiveId, final String toolId, final byte[] content, final String subdirectory,
			final String filename) {
		// TODO Auto-generated method stub

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void archiveContent(final String archiveId, final String toolId, final Object content, final String filename)
			throws ArchiveWriterException {

		// TODO only writes to system.out
		try {
			final JAXBContext jc = JAXBContext.newInstance(Object.class);
			final JAXBIntrospector introspector = jc.createJAXBIntrospector();
			final Marshaller marshaller = jc.createMarshaller();
			if (null == introspector.getElementName(content)) {
				final JAXBElement jaxbElement = new JAXBElement(new QName("ROOT"), content.getClass(), content);
				marshaller.marshal(jaxbElement, System.out);
			} else {
				marshaller.marshal(content, System.out);
			}
		} catch (final JAXBException e) {
			throw new ArchiveWriterException("Content could not be serialised", e);
		}

	}

}
