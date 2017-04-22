package org.sakaiproject.archiver.app.pages;

import org.apache.wicket.markup.html.basic.Label;

public class PreviousArchivesPage extends BasePage {

	private static final long serialVersionUID = 1L;

	public PreviousArchivesPage() {
		disableLink(this.previousArchivesLink);
	}

	@Override
	public void onInitialize() {
		super.onInitialize();

		add(new Label("instructions", "blah"));
	}
}
