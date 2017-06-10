package org.sakaiproject.archiver.util;

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Utility to convert an object into a basic HTML representation.
 *
 * Leverages ReflectionToStringBuilder and adds some basic styling.
 */
public class Htmlifier extends ToStringStyle {

	private static final long serialVersionUID = 1L;

	private Htmlifier() {
		setFieldSeparator("</td></tr>" + SystemUtils.LINE_SEPARATOR + "<tr><td>");

		setContentStart("<table class=\"table table-bordered table-condensed\">" + SystemUtils.LINE_SEPARATOR +
				"<thead><tr><th>Field</th><th>Data</th></tr></thead>" +
				"<tbody><tr><td>");

		setFieldNameValueSeparator("</td><td>");

		setContentEnd("</td></tr>" + SystemUtils.LINE_SEPARATOR + "</tbody></table>");

		setArrayContentDetail(true);
		setUseShortClassName(true);
		setUseClassName(false);
		setUseIdentityHashCode(false);
	}

	@Override
	public void appendDetail(final StringBuffer buffer, final String fieldName, final Object value) {
		if (value.getClass().getName().startsWith("java.lang")) {
			super.appendDetail(buffer, fieldName, value);
		} else {
			buffer.append(ReflectionToStringBuilder.toString(value, this));
		}
	}

	/**
	 * Serialise an object to HTML, with some styling
	 *
	 * @param obj the object to serialise
	 * @return a String of HTML
	 */
	public static String toHtml(final Object object) {
		final StringBuilder sb = new StringBuilder();
		sb.append(getHtmlStart());
		sb.append(ReflectionToStringBuilder.toString(object, new Htmlifier()));
		sb.append(getHtmlEnd());
		return sb.toString();
	}

	/**
	 * Gets the HTML to startup the HTML document. Uses bootstrap for a bit of bling.
	 *
	 * @return
	 */
	private static String getHtmlStart() {

		final StringBuilder sb = new StringBuilder();
		sb.append("<!DOCTYPE html>");
		sb.append("<html lang=\"en\">");
		sb.append("<head>");
		sb.append("<meta charset=\"utf-8\">");
		sb.append("<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">");
		sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
		sb.append("<link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css\">");
		sb.append("<link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap-theme.min.css\">");
		sb.append("<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js\"></script>");
		sb.append("<script src=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js\"></script>");
		sb.append("</head>");
		sb.append("<body>");
		sb.append("<div class=\"container\">");
		return sb.toString();
	}

	/**
	 * Gets the HTML to close off the document
	 *
	 * @return
	 */
	private static String getHtmlEnd() {

		final StringBuilder sb = new StringBuilder();
		sb.append("</div>");
		sb.append("</body>");
		sb.append("</html>");
		return sb.toString();
	}
}
