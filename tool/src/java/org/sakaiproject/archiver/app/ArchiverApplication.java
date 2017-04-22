package org.sakaiproject.archiver.app;

import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.settings.IExceptionSettings;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.sakaiproject.archiver.app.pages.CreateArchivePage;

public class ArchiverApplication extends WebApplication {

	@Override
	public void init() {
		super.init();

		// page mounting for bookmarkable URLs
		// mountPage("/grades", GradebookPage.class);
		// mountPage("/settings", SettingsPage.class);
		// mountPage("/importexport", ImportExportPage.class);
		// mountPage("/permissions", PermissionsPage.class);

		// remove the version number from the URL so that browser refreshes re-render the page
		// getRequestCycleSettings().setRenderStrategy(RenderStrategy.ONE_PASS_RENDER);

		// Configure for Spring injection
		getComponentInstantiationListeners().add(new SpringComponentInjector(this));

		// Don't throw an exception if we are missing a property, just fallback
		getResourceSettings().setThrowExceptionOnMissingResource(false);

		// Remove the wicket specific tags from the generated markup
		getMarkupSettings().setStripWicketTags(true);

		// Don't add any extra tags around a disabled link (default is <em></em>)
		getMarkupSettings().setDefaultBeforeDisabledLink(null);
		getMarkupSettings().setDefaultAfterDisabledLink(null);

		// On Wicket session timeout, redirect to main page
		// getApplicationSettings().setPageExpiredErrorPage(getHomePage());

		// show internal error page rather than default developer page
		// for production, set to SHOW_NO_EXCEPTION_PAGE
		getExceptionSettings().setUnexpectedExceptionDisplay(IExceptionSettings.SHOW_EXCEPTION_PAGE);

		// Intercept any unexpected error stacktrace and take to our page
		// getRequestCycleListeners().add(new AbstractRequestCycleListener() {
		// @Override
		// public IRequestHandler onException(final RequestCycle cycle, final Exception e) {
		// return new RenderPageRequestHandler(new PageProvider(new ErrorPage(e)));
		// }
		// });

		// cleanup the HTML
		getMarkupSettings().setStripWicketTags(true);
		getMarkupSettings().setStripComments(true);
		getMarkupSettings().setCompressWhitespace(true);

	}

	/**
	 * The main page for our app
	 *
	 * @see org.apache.wicket.Application#getHomePage()
	 */
	@Override
	public Class<CreateArchivePage> getHomePage() {
		return CreateArchivePage.class;
	}

}
