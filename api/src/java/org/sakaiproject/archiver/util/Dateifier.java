package org.sakaiproject.archiver.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

/**
 * Utility for working with dates
 */
public class Dateifier {

	private Dateifier() {
		throw new IllegalArgumentException("Utility class");
	}

	/**
	 * Format a date to ISO 8601 using default Locale
	 *
	 * @param date
	 * @return string or null if date is null
	 */
	public static String toIso8601(final Date date) {
		return toIso8601(date, Locale.getDefault());
	}

	/**
	 * Format a date to ISO 8601 using specific Locale
	 *
	 * @param date
	 * @param locale the locale to format the date to
	 * @return string or null if date is null
	 */
	public static String toIso8601(final Date date, final Locale locale) {
		if (date == null) {
			return null;
		}

		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
				.withLocale(locale)
				.withZone(ZoneId.systemDefault());

		final Instant instant = date.toInstant();
		return formatter.format(instant);
	}

}
