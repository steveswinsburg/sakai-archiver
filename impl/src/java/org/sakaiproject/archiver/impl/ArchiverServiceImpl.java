package org.sakaiproject.archiver.impl;

import org.sakaiproject.archiver.api.ArchiverService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ArchiverServiceImpl implements ArchiverService {

	public void init() {
		log.info("ArchiverService started");
	}
}
