package org.sakaiproject.archiver.app.pages;

/**
 * Admin view
 */
public class AdminPage extends BasePage {

	private static final long serialVersionUID = 1L;

	public AdminPage() {
		disableLink(this.adminLink);
	}

	@Override
	public void onInitialize() {
		super.onInitialize();

	}

}
