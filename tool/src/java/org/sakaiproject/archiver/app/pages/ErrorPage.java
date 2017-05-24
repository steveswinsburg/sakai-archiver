package org.sakaiproject.archiver.app.pages;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.markup.html.basic.Label;

import lombok.extern.slf4j.Slf4j;

/**
 * Page displayed when a runtime exception occurs
 *
 * @author Steve Swinsburg (steve.swinsburg@gmail.com)
 *
 */
@Slf4j
public class ErrorPage extends BasePage {

	private static final long serialVersionUID = 1L;

	public ErrorPage(final Exception e) {

		final String stacktrace = ExceptionUtils.getStackTrace(e);
		log.error(stacktrace);

		add(new Label("stacktrace", stacktrace));

	}
}
