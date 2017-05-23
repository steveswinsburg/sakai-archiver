package org.sakaiproject.archiver.api.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
		testObject.setParam2(2);
		testObject.setParam4(false);
		testObject.setParam3(new Date(888888888));

		final List<String> list = new ArrayList<>();
		list.add("elem1");
		list.add("elem2");
		testObject.setList1(list);

		final Map<Integer, Date> map1 = new LinkedHashMap<Integer, Date>();
		map1.put(1, new Date(111111111));
		map1.put(9, new Date(999999999));
		map1.put(2, new Date(222222222));
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
		sb.append("\"param3\":\"Jan 11, 1970 4:54:48 PM\",");
		sb.append("\"param4\":false,");
		sb.append("\"list1\":[\"elem1\",\"elem2\"],");
		sb.append("\"map1\":{");
		sb.append("\"1\":\"Jan 2, 1970 4:51:51 PM\",");
		sb.append("\"9\":\"Jan 12, 1970 11:46:39 PM\",");
		sb.append("\"2\":\"Jan 3, 1970 11:43:42 PM\"");
		sb.append("}");
		sb.append("}");
		return sb.toString();
		// {"param1":"param1","param2":2,"param3":"Jan 11, 1970 4:54:48 PM","param4":false,"list1":["elem1","elem2"],"map1":{"1":"Jan 2,
		// 1970 4:51:51 PM","9":"Jan 12, 1970 11:46:39 PM","2":"Jan 3, 1970 11:43:42 PM"}}
	}

}
