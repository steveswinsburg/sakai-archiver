package org.sakaiproject.archiver.impl;

import java.io.File;

/**
 * Manages the assembly of the top level index page for an archive path
 *
 * Note that this uses jsTree library from https://www.jstree.com/. Please see that site for applicable licenses
 */
public class IndexBuilder {

	StringBuilder sb = new StringBuilder();
	File archiveBase;

	/**
	 * Initialise the {@link IndexBuilder}
	 *
	 * @param path root dir of the archive
	 */
	public IndexBuilder(final String path) {
		this.archiveBase = new File(path);
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

		this.sb.append("<li>");
		this.sb.append(folder.getName());
		this.sb.append("<ul">");
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
		this.sb.append("<li>");
		this.sb.append(file.getName());
		this.sb.append("</li>");
	}

	private void getStart() {

		this.sb.append("<!DOCTYPE html>");
		this.sb.append("<html lang=\"en\">");
		this.sb.append("<head>");
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

		this.sb.append("<script>$('#jstree').jstree();</script>");

		this.sb.append("</head>");
		this.sb.append("<body>");
		this.sb.append("<div class=\"container\" id=\"tree\">");
		this.sb.append("<ul>");
	}

	private void getEnd() {
		this.sb.append("</ul></div></body></html>");
	}

}
