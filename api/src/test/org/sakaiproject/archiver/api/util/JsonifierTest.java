package org.sakaiproject.archiver.api.util;

import static org.junit.Assert.assertEquals;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.archiver.util.Jsonifier;

import lombok.Getter;
import lombok.Setter;

/**
 * Test for the {@link Jsonifier} utility
 */
public class JsonifierTest {

	Date date1;
	Date date2;
	Date date3;
	Date date4;

	@Before
	public void setup() {
		this.date1 = getDate(4);
		this.date2 = getDate(-6);
		this.date3 = getDate(17);
		this.date4 = getDate(-11);
	}

	@Test
	public void should_serialiseToJson_when_simpleStringProvided() {
		final String s = new String("s");
		final String result = Jsonifier.toJson(s);
		final String expected = "\"s\"";
		assertEquals(expected, result);
	}

	@Test
	public void should_serialiseToJson_when_complexObjectProvided() {

		final TestObject testObject = new TestObject();
		testObject.setParam1("param1");
		testObject.setParam2(2);
		testObject.setParam4(false);

		testObject.setParam3(this.date1);

		final List<String> list = new ArrayList<>();
		list.add("elem1");
		list.add("elem2");
		testObject.setList1(list);

		final Map<Integer, Date> map1 = new LinkedHashMap<Integer, Date>();
		map1.put(1, this.date2);
		map1.put(9, this.date3);
		map1.put(2, this.date4);
		testObject.setMap1(map1);

		final String result = Jsonifier.toJson(testObject);

		final String expected = getExpectedJson();

		assertEquals(expected, result);
	}

	class TestObject {

		@Getter
		@Setter
		private String param1;

		@Getter
		@Setter
		private int param2;

		@Getter
		@Setter
		private Date param3;

		@Getter
		@Setter
		private boolean param4;

		@Getter
		@Setter
		private List<String> list1;

		@Getter
		@Setter
		private Map<Integer, Date> map1;
	}

	private String getExpectedJson() {
		final StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append("\"param1\":\"param1\",");
		sb.append("\"param2\":2,");
		sb.append("\"param3\":\"" + expectedDateSerialisation(this.date1) + "\",");
		sb.append("\"param4\":false,");
		sb.append("\"list1\":[\"elem1\",\"elem2\"],");
		sb.append("\"map1\":{");
		sb.append("\"1\":\"" + expectedDateSerialisation(this.date2) + "\",");
		sb.append("\"9\":\"" + expectedDateSerialisation(this.date3) + "\",");
		sb.append("\"2\":\"" + expectedDateSerialisation(this.date4) + "\"");
		sb.append("}");
		sb.append("}");
		return sb.toString();

	}

	private Date getDate(final int hourOffset) {
		final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Australia/Canberra"));
		calendar.add(Calendar.HOUR_OF_DAY, hourOffset);
		return calendar.getTime();
	}

	private String expectedDateSerialisation(final Date date) {
		final DateFormat dateFormat = new SimpleDateFormat("MMM dd, YYYY h:mm:ss a");
		return dateFormat.format(date);
	}
}
