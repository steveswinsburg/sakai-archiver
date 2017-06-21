package org.sakaiproject.archiver.provider.util;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * A collection of methods to format grades.
 */
@Slf4j
public class FormatHelper {

	/**
	 * The value is a double (ie 12.34542) that needs to be formatted as a percentage with 'n' decimal places precision. And drop off any .0
	 * if no decimal places.
	 *
	 * @param score as a double
	 * @param n as an int
	 * @return double to n decimal places
	 */
	public static String formatDoubleToDecimal(final Double score, final int n) {
		final NumberFormat df = NumberFormat.getInstance();
		df.setMinimumFractionDigits(0);
		df.setMaximumFractionDigits(n);
		df.setGroupingUsed(false);
		df.setRoundingMode(RoundingMode.HALF_DOWN);

		return formatGrade(df.format(score));
	}

	/**
	 * Format a grade, e.g. 00 => 0 0001 => 1 1.0 => 1 1.25 => 1.25
	 *
	 * @param grade
	 * @return
	 */
	public static String formatGrade(final String grade) {
		if (StringUtils.isBlank(grade)) {
			return "";
		}

		String s = null;
		try {
			final Double d = Double.parseDouble(grade);

			final DecimalFormat df = (DecimalFormat) NumberFormat.getInstance(I18n.getUserPreferredLocale());
			df.setMinimumFractionDigits(0);
			df.setGroupingUsed(false);

			s = df.format(d);
		} catch (final NumberFormatException e) {
			log.debug("Bad format, returning original string: " + grade);
			s = grade;
		}

		return trimZero(s);
	}

	/**
	 * Trim any .0 from a grade
	 *
	 * @param grade
	 * @return
	 */
	public static String trimZero(final String grade) {
		return StringUtils.removeEnd(grade, ".0");
	}

}
