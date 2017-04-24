package org.sakaiproject.archiver.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
	
	private final Map<String, Archiveable> registry = new HashMap<>();
	
	/**
	 * Get an instance of the {@link ArchiverRegistry} so that services can register/unregister themselves
	 * @return
	 */
	public static synchronized ArchiverRegistry getInstance() {
		if (instance == null) {
			instance = new ArchiverRegistry();
		}
		return instance;
	}
	
	/**
	 * Register an {@link Archiveable} service
	 * @param service
	 */
	public synchronized void register(final Archiveable archiveable) {
		String key = getKey(archiveable);
		if(!isRegistered(key)){
			log.warn("ArchiverRegistry already contains %S", key);
		} else {
			registry.put(key, archiveable);
		}
	}
	
	/**
	 * Unregister an {@link Archiveable} service
	 * @param service
	 */
	public synchronized void unregister(final Archiveable archiveable) {
		String key = getKey(archiveable);
		if(!isRegistered(key)){
			registry.remove(key);
		} 
	}
   
	/**
	 * Get the list of registered services that are {@link Archiveable}
	 * @return {@link List} of registered {@link Archiveable} services
	 */
	public List<Archiveable> getArchiveables() {
		return this.registry.values().stream().collect(Collectors.toList());
	}
	
	/**
	 * Checks if a service is registered already
	 * @param name 
	 * @return
	 */
	private boolean isRegistered(final String name) {
		return registry.containsKey(name);
	}
	
	/**
	 * Common way to get a key for a service
	 * @param archiveable
	 * @return
	 */
    private String getKey(Archiveable archiveable){
    	return archiveable.getClass().getCanonicalName();
    }
	
}
