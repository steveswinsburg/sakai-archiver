package org.sakaiproject.archiver.util;

import java.util.Arrays;

/**
 * Utility to sanitise a string so that it doesn't contain non-standard characters.
 */
public class Sanitiser {

	/**
	 * Replace illegal chars in the supplied string with _
	 *
	 * @param filename
	 * @return
	 */
	public static String sanitise(final String string) {
		return string.replaceAll("[^a-zA-Z0-9.\" \"-]", "_");
	}

	/**
	 * Replace illegal chars in the supplied strings with _
	 *
	 * @param filename varargs
	 * @return
	 */
	public static String[] sanitise(final String... strings) {
		return Arrays.stream(strings)
				.map(s -> sanitise(s))
				.toArray(String[]::new);
	}
}
