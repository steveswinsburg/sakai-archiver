
package org.sakaiproject.archiver.provider.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.sakaiproject.util.ResourceLoader;

/**
 * Get messages from the relevant property file
 */
public class I18n {

	private static final String BASE_NAME = "org.sakaiproject.archiver.provider.export";

	/**
	 * Get a simple message from the bundle
	 *
	 * @param key
	 * @return
	 */
	public static String getString(final String key) {
		try {
			return ResourceBundle.getBundle(BASE_NAME, getUserPreferredLocale()).getString(key);
		} catch (final MissingResourceException e) {
			return '!' + key + '!';
		}
	}

	/**
	 * Get a parameterised message from the bundle and perform the parameter substitution on it
	 *
	 * @param key
	 * @return
	 */
	public static String getString(final String key, final Object... arguments) {
		return MessageFormat.format(getString(key), arguments);
	}

	/**
	 * Helper to get the Locale from Sakai
	 * 
	 * @return the locale
	 */
	private static Locale getUserPreferredLocale() {
		final ResourceLoader rl = new ResourceLoader();
		return rl.getLocale();
	}
}
