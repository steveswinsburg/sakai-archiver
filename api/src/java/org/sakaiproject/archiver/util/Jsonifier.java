package org.sakaiproject.archiver.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Utility to convert an object into a JSON representation
 */
public class Jsonifier {

	/**
	 * Serialise an object to JSON
	 *
	 * @param obj the object to serialise
	 * @return a String of JSON
	 */
	public static String toJson(final Object obj) {
		final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
		return gson.toJson(obj);
	}
}
