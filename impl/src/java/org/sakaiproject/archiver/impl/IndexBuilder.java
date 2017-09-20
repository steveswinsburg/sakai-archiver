package org.sakaiproject.archiver.impl;

import java.io.File;

import org.apache.commons.lang3.StringUtils;

/**
 * Manages the assembly of the top level index page for an archive path
 *
 * Note that this uses jsTree library from https://www.jstree.com/. Please see that site for applicable licenses.
 */
public class IndexBuilder {

	private final StringBuilder sb = new StringBuilder();
	private final File archiveBase;
	private final String title;

	private int dirCount = 0;

	/**
	 * Initialise the {@link IndexBuilder}
	 *
	 * @param path root dir of the archive
	 */
	public IndexBuilder(final String path, final String title) {
		this.archiveBase = new File(path);
		this.title = title;
	}

	/**
	 * Build the index
	 *
	 * @return
	 */
	public String build() {

		getStart();
		renderDirectory(this.archiveBase);
		getEnd();

		return this.sb.toString();
	}

	/**
	 * Render a directory and all children
	 *
	 * @param folder
	 * @param sb
	 */
	private void renderDirectory(final File folder) {

		if (this.dirCount == 0) {
			this.sb.append("<li data-jstree='{\"opened\":true, \"selected\":true, \"icon\":\"glyphicon glyphicon-folder-open\"}'>");
		} else {
			this.sb.append("<li data-jstree='{\"icon\":\"glyphicon glyphicon-folder-open\"}'>");
		}
		this.dirCount++;
		this.sb.append(folder.getName());
		this.sb.append("<ul>");
		for (final File file : folder.listFiles()) {
			if (file.isDirectory()) {
				renderDirectory(file);
			} else {
				renderFile(file);
			}
		}
		this.sb.append("</ul>");

	}

	/**
	 * Render a file
	 *
	 * @param file
	 * @param sb
	 */
	private void renderFile(final File file) {
		this.sb.append("<li data-jstree='{\"icon\":\"glyphicon glyphicon-file\"}'>");
		this.sb.append("<a href=\"" + getRelativePath(file) + "\">");
		this.sb.append(file.getName());
		this.sb.append("</a></li>");
	}

	private void getStart() {

		this.sb.append("<!DOCTYPE html>");
		this.sb.append("<html lang=\"en\">");
		this.sb.append("<head>");
		this.sb.append("<title>" + this.title + "</title>");
		this.sb.append("<meta charset=\"utf-8\">");
		this.sb.append("<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">");
		this.sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
		this.sb.append("<link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css\">");
		this.sb.append("<link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap-theme.min.css\">");
		this.sb.append(
				"<link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/jstree/3.2.1/themes/default/style.min.css\"/>");

		this.sb.append("<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js\"></script>");
		this.sb.append("<script src=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js\"></script>");
		this.sb.append("<script src=\"https://cdnjs.cloudflare.com/ajax/libs/jstree/3.2.1/jstree.min.js\"></script>");

		this.sb.append(
				"<script>$(document).ready(function() { $('#tree').jstree({ \"plugins\" : [ \"state\" ] }).bind('changed.jstree', function (e, data) { if(data.node) { var href = data.node.a_attr.href; if(href !== '#') { window.open(href, '_self'); }}})});</script>");

		this.sb.append("</head>");
		this.sb.append("<body>");
		this.sb.append("<div class=\"container\">");
		this.sb.append("<h1>" + this.title + "</h1>");
		this.sb.append("<div id=\"tree\">");
		this.sb.append("<ul>");
	}

	private void getEnd() {
		this.sb.append("</ul></div></div></body></html>");
	}

	/**
	 * Determines a relative path to the file from the top level index.html file
	 *
	 * @param file
	 * @return relative path
	 */
	private String getRelativePath(final File file) {
		return StringUtils.removeStart(StringUtils.removeStart(file.getAbsolutePath(), this.archiveBase.getAbsolutePath()), "/");
	}

}
