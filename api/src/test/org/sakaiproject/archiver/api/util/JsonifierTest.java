package org.sakaiproject.archiver.api.util;

import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.sakaiproject.archiver.util.Jsonifier;

import lombok.Getter;
import lombok.Setter;

/**
 * Test for the {@link Jsonifier} utility
 */
public class JsonifierTest {

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
		testObject.setParam2("param2");

		final List<String> list = new ArrayList<>();
		list.add("elem1");
		list.add("elem2");
		testObject.setList1(list);

		final String result = Jsonifier.toJson(testObject);
		final String expected = "{\"param1\":\"param1\",\"param2\":\"param2\",\"list1\":[\"elem1\",\"elem2\"]}";
		assertEquals(expected, result);
	}
	
	@Test
	public void should_serialiseToJson_when_complexObjectWithDateProvided() {
		
		final TestObjectWithDate testObject = new TestObjectWithDate();
		testObject.setParam1("param1");
		Date currentDate = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		testObject.setDate(currentDate);
		
		final String result = Jsonifier.toJson(testObject, "yyyy-MM-dd HH:mm:ss.SSS");
		final String expected = "{\"param1\":\"param1\",\"date\":\"" + sdf.format(currentDate) + "\"}";
		assertEquals(expected, result);
	}

	class TestObject {

		@Getter
		@Setter
		private String param1;

		@Getter
		@Setter
		private String param2;

		@Getter
		@Setter
		private List<String> list1;
	}
	
	class TestObjectWithDate {
		@Getter
		@Setter
		private String param1;
		
		@Getter
		@Setter
		private Date date;
	}

}
