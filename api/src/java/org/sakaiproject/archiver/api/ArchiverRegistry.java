package org.sakaiproject.archiver.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.sakaiproject.archiver.spi.Archiveable;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Registry of all tools/services which are {@link Archiveable}
 *
 * @since 12.0
 * @author Steve Swinsburg
 */
@Slf4j
public class ArchiverRegistry {

	private static ArchiverRegistry instance;

	// holds the map of toolId to service class
	// must be thread safe
	@Getter
	private final Map<String, Archiveable> registry = new ConcurrentHashMap<>();

	private ArchiverRegistry() {
		// no instances
	}

	/**
	 * Get an instance of the {@link ArchiverRegistry} so that services can register/unregister themselves
	 *
	 * @return the instance as a singleton
	 */
	public static synchronized ArchiverRegistry getInstance() {
		if (instance == null) {
			instance = new ArchiverRegistry();
		}
		return instance;
	}

	/**
	 * Register an {@link Archiveable} service for a toolId
	 *
	 * @param toolId the toolId to register this archiver for
	 * @param archiveable the service to register
	 */
	public synchronized void register(final String toolId, final Archiveable archiveable) {
		if (isRegistered(toolId)) {
			log.warn("ArchiverRegistry already contains a registration for {}", toolId);
		} else {
			this.registry.put(toolId, archiveable);
			log.info("Added registration for {} as {}", toolId, archiveable.getClass().getCanonicalName());
		}
	}

	/**
	 * Unregister an {@link Archiveable} service
	 *
	 * @param toolId the toolId to be unregistered
	 */
	public synchronized void unregister(final String toolId) {
		if (isRegistered(toolId)) {
			log.info("Unregistering {}", toolId);
			this.registry.remove(toolId);
		}
	}

	/**
	 * Checks if a service is registered already for this toolId
	 *
	 * @param toolId the toolId to check the registry for
	 * @return
	 */
	private boolean isRegistered(final String toolId) {
		return this.registry.containsKey(toolId);
	}

}
