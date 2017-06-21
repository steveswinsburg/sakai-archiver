package org.sakaiproject.archiver.api.util;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.sakaiproject.archiver.util.Htmlifier;

import lombok.Getter;
import lombok.Setter;

/**
 * Test for the {@link Htmlifier} utility
 */
public class HtmlifierTest {

	@Test
	public void should_serialiseToHtml_when_simpleObjectProvided() {
		final Person person = new Person("Bob Smith");
		final String html = Htmlifier.toHtml(person);
		assertTrue(html.contains("Bob Smith"));
		assertTrue(html.contains("<td>phones</td><td></td>"));
	}

	@Test
	public void should_serialiseToHtml_when_simpleObjectWithListProvided() {
		final Person person = new Person("Bob Smith");

		final List<String> phones = new ArrayList<String>();
		phones.add("213-555-1212");
		person.setPhones(phones);

		final String html = Htmlifier.toHtml(person);
		assertTrue(html.contains("Bob Smith"));
		assertTrue(html.contains("<td>phones</td><td>213-555-1212</td>"));

	}

	@Test
	public void should_serialiseToHtml_when_nestedObjectProvided() {
		final Person person = new Person("Bob Smith");
		final List<String> phones = new ArrayList<String>();
		phones.add("213-555-1212");
		person.setPhones(phones);

		final Person friend = new Person("Mary Jones");
		friend.phones.add("408-555-1212");
		friend.phones.add("415-555-1212");
		person.bestFriend = friend;

		final String html = Htmlifier.toHtml(person);
		assertTrue(html.contains("Bob Smith"));
		assertTrue(html.contains("Mary Jones"));

		// check for nested table
		assertTrue(html.contains("bestFriend</td><td><table"));

	}

	class Person {

		@Getter
		@Setter
		private String id;

		@Getter
		@Setter
		private String name;

		@Getter
		@Setter
		private List<String> phones;

		@Getter
		@Setter
		private Person bestFriend;

		public Person(final String name) {
			this.id = "1234";
			this.name = name;
			this.phones = new ArrayList<String>();
		}
	}
}
