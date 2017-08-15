package org.sakaiproject.archiver.app.pages;

import org.sakaiproject.archiver.app.panels.ShowArchives;

/**
 * Shows previous archives for the site.
 *
 * Uses the {@link ShowArchives} panel
 */
public class ArchiveHistoryPage extends BasePage {

	private static final long serialVersionUID = 1L;

	public ArchiveHistoryPage() {
		disableLink(this.archiveHistoryLink);
	}

	@Override
	public void onInitialize() {
		super.onInitialize();

		add(new ShowArchives("showArchives"));
	}

}
