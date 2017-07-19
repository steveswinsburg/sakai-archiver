package org.sakaiproject.archiver.provider.model;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import lombok.Getter;
import lombok.Setter;

/**
 * Wrapper for the data in a roster. Fields are ordered as they appear in the export.
 */
public class RosterEntry {

	/**
	 * eid of the user this entry is for
	 */
	@Getter
	@Setter
	private String eid;

	@Getter
	@Setter
	private Map<String, String> fields = new LinkedHashMap<>();

	@Getter
	@Setter
	private String photoUrl = null;

	public RosterEntry() {
	}

	public void addField(final String label, final String value) {
		this.fields.put(label, value);
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
	}

}
