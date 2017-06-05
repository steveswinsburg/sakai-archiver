package org.sakaiproject.archiver.api.util;

import static org.junit.Assert.assertEquals;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.archiver.util.Dateifier;

/**
 * Test for the {@link Dateifier} utility
 */
public class DateifierTest {

	Date date1;
	Date date2;
	Date date3;
	Date date4;

	@Before
	public void setup() {
		this.date1 = getDate(41);
		this.date2 = getDate(-6);
		this.date3 = getDate(170);
		this.date4 = getDate(-17);
	}

	@Test
	public void should_convertToIso8601_when_date1Provided() {
		final String result = Dateifier.toIso8601(this.date1);
		final String expected = getExpectedDateString(this.date1);
		assertEquals(expected, result);
	}

	@Test
	public void should_convertToIso8601_when_date2Provided() {
		final String result = Dateifier.toIso8601(this.date2);
		final String expected = getExpectedDateString(this.date2);
		assertEquals(expected, result);
	}

	@Test
	public void should_convertToIso8601_when_date3Provided() {
		final String result = Dateifier.toIso8601(this.date3);
		final String expected = getExpectedDateString(this.date3);
		assertEquals(expected, result);
	}

	@Test
	public void should_convertToIso8601_when_date4Provided() {
		final String result = Dateifier.toIso8601(this.date4);
		final String expected = getExpectedDateString(this.date4);
		assertEquals(expected, result);
	}

	private Date getDate(final int hourOffset) {
		final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Australia/Canberra"));
		calendar.add(Calendar.HOUR_OF_DAY, hourOffset);
		return calendar.getTime();
	}

	private String getExpectedDateString(final Date date) {
		final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return dateFormat.format(date);
	}
}
