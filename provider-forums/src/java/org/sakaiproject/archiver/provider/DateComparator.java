package org.sakaiproject.archiver.provider;

import java.util.Comparator;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.sakaiproject.api.app.messageforums.Message;

public class DateComparator implements Comparator<Message> {

	@Override
	public int compare(final Message m1, final Message m2) {
		return new CompareToBuilder()
				.append(m1.getCreated(), m2.getCreated())
				.toComparison();
	}

}
