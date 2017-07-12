package org.sakaiproject.archiver.util;

import java.util.List;

import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 * Utility to convert an object into a basic HTML representation.
 *
 * Leverages {@link ReflectionToStringBuilder} and {@link RecursiveToStringStyle} and adds some basic styling.
 */
public class Htmlifier extends RecursiveToStringStyle {

	private static final long serialVersionUID = 1L;

	private Htmlifier() {
		setFieldSeparator("</td></tr>" + SystemUtils.LINE_SEPARATOR + "<tr><td>");

		setContentStart("<table class=\"table table-bordered table-condensed table-sm\">" + SystemUtils.LINE_SEPARATOR +
				"<thead><tr><th>Field</th><th>Data</th></tr></thead>" +
				"<tbody><tr><td>");

		setFieldNameValueSeparator("</td><td>");

		setContentEnd("</td></tr>" + SystemUtils.LINE_SEPARATOR + "</tbody></table>");

		setUseShortClassName(true);
		setUseClassName(false);
		setUseIdentityHashCode(false);
		setArrayContentDetail(true);

		// remove the { and } around arrays
		setArrayStart(null);
		setArrayEnd(null);

	}

	/**
	 * Serialise an object to HTML, with some styling.
	 *
	 * @param object the object to serialise
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
	 * Serialise a list of objects to HTML, with some styling.
	 *
	 * @param objects the objects to serialise. Each one will get it's own rendering.
	 * @return a String of HTML
	 */
	public static String toHtml(final List<?> objects) {
		final StringBuilder sb = new StringBuilder();
		sb.append(getHtmlStart());
		objects.forEach(o -> {
			sb.append(ReflectionToStringBuilder.toString(o, new Htmlifier()));
		});
		sb.append(getHtmlEnd());
		return sb.toString();
	}

	/**
	 * Add html start and end to a html body string
	 *
	 * @param htmlBody
	 * @return
	 */
	public static String toHtml(final String htmlBody) {
		final StringBuilder sb = new StringBuilder();
		sb.append(getHtmlStart());
		sb.append(htmlBody);
		sb.append(getHtmlEnd());
		return sb.toString();
	}

	public static String addSiteHeader(final String html, final String heading) {
		// TODO Use Jsoup to add a header to the very top of the body
		final Document doc = Jsoup.parse(html);
		final Elements divs = doc.select("div");
		divs.first().prepend("<h2 align=\"center\">" + heading + "</h2>");
		return doc.html();
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
