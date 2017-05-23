package org.sakaiproject.archiver.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Utility to convert an object into a JSON representation
 */
public class Jsonifier {

	public static String toJson(final Object obj) {
		final Gson gson = new GsonBuilder().create();
		return gson.toJson(obj);
	}
}
