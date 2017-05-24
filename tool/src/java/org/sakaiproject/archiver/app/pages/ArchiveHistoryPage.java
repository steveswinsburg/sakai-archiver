package org.sakaiproject.archiver.app.pages;

import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PageableListView;
import org.sakaiproject.archiver.dto.Archive;

public class ArchiveHistoryPage extends BasePage {

	private static final long serialVersionUID = 1L;

	public ArchiveHistoryPage() {
		disableLink(this.archiveHistoryLink);
	}

	@Override
	public void onInitialize() {
		super.onInitialize();

		// get list of archives for this site
		final List<Archive> archives = this.businessService.getArchives();

		final PageableListView<Archive> listView = new PageableListView<Archive>("archiveHistoryView", archives, 10) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(final ListItem<Archive> item) {
				final Archive archive = item.getModelObject();

				item.add(new Label("dateCompleted", archive.getEndDate().toString()));
				item.add(new Label("status", archive.getStatus().toString()));
				item.add(new Label("creator", archive.getUserUuid()));
				item.add(new Label("downloadLink", archive.getZipPath()));
			}

		};
		add(listView);

	}

}
