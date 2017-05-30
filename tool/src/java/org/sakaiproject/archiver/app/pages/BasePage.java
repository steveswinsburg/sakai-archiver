package org.sakaiproject.archiver.app.pages;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.head.PriorityHeaderItem;
import org.apache.wicket.markup.head.StringHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.archiver.app.business.ArchiverBusinessService;
import org.sakaiproject.archiver.app.components.RichFeedbackPanel;

import lombok.extern.slf4j.Slf4j;

/**
 * Base page for our app
 *
 * @author Steve Swinsburg (steve.swinsburg@gmail.com)
 *
 */
@Slf4j
public class BasePage extends WebPage {

	private static final long serialVersionUID = 1L;

	@SpringBean(name = "org.sakaiproject.archiver.app.business.ArchiverBusinessService")
	protected transient ArchiverBusinessService businessService;

	Link<Void> createArchiveLink;
	Link<Void> archiveHistoryLink;

	public final RichFeedbackPanel feedbackPanel;

	/**
	 * The current user
	 */
	protected String currentUserUuid;

	public BasePage() {
		log.debug("BasePage()");

		// setup some data that can be shared across all pages
		this.currentUserUuid = this.businessService.getCurrentUser().getId();

		// set locale
		setUserPreferredLocale();

		// nav container
		final WebMarkupContainer nav = new WebMarkupContainer("pageNav");

		// create archive
		this.createArchiveLink = new Link<Void>("createArchiveLink") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick() {
				setResponsePage(CreateArchivePage.class);
			}

		};
		this.createArchiveLink.add(new Label("screenreaderlabel", getString("link.screenreader.tabnotselected")));
		nav.add(this.createArchiveLink);

		// archive history
		this.archiveHistoryLink = new Link<Void>("archiveHistoryLink") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick() {
				setResponsePage(ArchiveHistoryPage.class);
			}

		};
		this.archiveHistoryLink.add(new Label("screenreaderlabel", getString("link.screenreader.tabnotselected")));
		nav.add(this.archiveHistoryLink);

		add(nav);

		// Add a FeedbackPanel for displaying our messages
		this.feedbackPanel = new RichFeedbackPanel("feedback");
		add(this.feedbackPanel);

		isArchiveInProgress();

	}

	/**
	 * Helper to clear the feedback panel display from any child component
	 */
	public void clearFeedback() {
		this.feedbackPanel.clear();
	}

	/**
	 * This block adds the required wrapper markup to style it like a Sakai tool. Add to this any additional CSS or JS references that you
	 * need.
	 *
	 */
	@Override
	public void renderHead(final IHeaderResponse response) {
		super.renderHead(response);

		final String version = this.businessService.getSakaiProperty("portal.cdn.version", "");

		// get the Sakai skin header fragment from the request attribute
		final HttpServletRequest request = (HttpServletRequest) getRequest().getContainerRequest();

		response.render(new PriorityHeaderItem(JavaScriptHeaderItem
				.forReference(getApplication().getJavaScriptLibrarySettings().getJQueryReference())));

		response.render(StringHeaderItem.forString((String) request.getAttribute("sakai.html.head")));
		response.render(OnLoadHeaderItem.forScript("setMainFrameHeight( window.name )"));

		// Tool additions (at end so we can override if required)
		response.render(StringHeaderItem
				.forString("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />"));

		// Shared JavaScript and stylesheets
		// Force Wicket to use Sakai's version of jQuery
		response.render(
				new PriorityHeaderItem(
						JavaScriptHeaderItem
								.forUrl(String.format("/library/webjars/jquery/1.11.3/jquery.min.js?version=%s", version))));

		// And pair this instance of jQuery with a Bootstrap version we've tested with
		response.render(
				new PriorityHeaderItem(
						JavaScriptHeaderItem
								.forUrl(String.format("/library/webjars/bootstrap/3.3.7/js/bootstrap.min.js?version=%s", version))));

		// Some style overrides
		response.render(CssHeaderItem.forUrl(String.format("/archiver-app/styles/archiver.css?version=%s", version)));

	}

	/**
	 * Helper to disable a link. Add the Sakai class 'current'.
	 */
	protected void disableLink(final Link<Void> l) {
		l.add(new AttributeAppender("class", new Model<String>("current"), " "));
		l.replace(new Label("screenreaderlabel", getString("link.screenreader.tabselected")));
		l.setEnabled(false);
	}

	/**
	 * Allow overrides of the user's locale
	 */
	public void setUserPreferredLocale() {
		final Locale locale = this.businessService.getUserPreferredLocale();
		log.debug("User preferred locale: " + locale);
		getSession().setLocale(locale);
	}

	/**
	 * Sets the feedback panel message that an archive is in progress
	 */
	private void isArchiveInProgress() {
		if (this.businessService.isArchiveInProgress()) {
			info("Archive is in progress");
		}
	}

}
