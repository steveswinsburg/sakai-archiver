package org.sakaiproject.archiver.api;

import java.util.ArrayList;
import java.util.List;
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

	// holds the map of toolId to service classes for that toolId
	// must be thread safe
	@Getter
	private final Map<String, List<Archiveable>> registry = new ConcurrentHashMap<>();

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
	 * Register an {@link Archiveable} service
	 *
	 * Multiple archiver services can call this for the same toolId if they share something in common.
	 *
	 * Normally they would also have the same 'name' field but they don't have to.
	 *
	 * @param archiveable the service to register
	 */
	public synchronized void register(final Archiveable archiveable) {
		final String toolId = archiveable.getToolId();
		final List<Archiveable> registrations = this.registry.getOrDefault(toolId, new ArrayList<>());
		registrations.add(archiveable);
		this.registry.put(toolId, registrations);
		log.info("Added registration for {} as {}", toolId, archiveable.getClass().getCanonicalName());
	}

	/**
	 * Unregister an {@link Archiveable} service
	 *
	 * @param toolId the toolId to be unregistered
	 */
	public synchronized void unregister(final String toolId) {
		if (isRegistered(toolId)) {
			log.info("Unregistering services for {}", toolId);
			this.registry.remove(toolId);
		}
	}

	/**
	 * Unregister all {@link Archiveable} services
	 */
	protected synchronized void unregisterAll() {
		getRegistry().keySet().forEach(toolId -> unregister(toolId));
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
