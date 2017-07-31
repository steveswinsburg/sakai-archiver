package org.sakaiproject.archiver.provider.model;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import lombok.Getter;
import lombok.Setter;

/**
 * Wrapper for the data in a roster. Fields are ordered as they appear in the export. A few fields are split out so they can be treated
 * specially
 */
public class RosterEntry {

	/**
	 * eid of the user this entry is for
	 */
	@Getter
	@Setter
	private String eid;

	/**
	 * Display name of the user
	 */
	@Getter
	@Setter
	private String displayName;

	/**
	 * Role of the user
	 */
	@Getter
	@Setter
	private String role;

	/**
	 * Email address of the user
	 */
	@Getter
	@Setter
	private String emailAddress;

	/**
	 * URL of the user's photo
	 */
	@Getter
	@Setter
	private String photoUrl = null;

	/**
	 * The rest of the fields
	 */
	@Getter
	@Setter
	private Map<String, String> fields = new LinkedHashMap<>();

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
