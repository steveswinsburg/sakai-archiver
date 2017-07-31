package org.sakaiproject.archiver.app.panels;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PageableListView;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.archiver.api.Status;
import org.sakaiproject.archiver.app.business.ArchiverBusinessService;
import org.sakaiproject.archiver.dto.Archive;
import org.sakaiproject.archiver.exception.ArchiveNotFoundException;
import org.sakaiproject.archiver.exception.ZipNotFoundException;
import org.sakaiproject.archiver.util.Dateifier;

import lombok.extern.slf4j.Slf4j;

/**
 * Panel to show archives. Used in a few places.
 */
@Slf4j
public class ShowArchives extends Panel {

	private static final long serialVersionUID = 1L;

	private final String siteId;

	@SpringBean(name = "org.sakaiproject.archiver.app.business.ArchiverBusinessService")
	private transient ArchiverBusinessService businessService;

	public ShowArchives(final String id) {
		super(id);
		this.siteId = this.businessService.getCurrentSiteId();
	}

	public ShowArchives(final String id, final IModel<String> model) {
		super(id, model);
		this.siteId = model.getObject();
	}

	@Override
	public void onInitialize() {
		super.onInitialize();

		log.debug("Archives for: " + this.siteId);

		// get list of archives
		final List<Archive> archives = this.businessService.getArchives(this.siteId);
		log.debug("Count: " + archives.size());

		// wrap the table
		final WebMarkupContainer table = new WebMarkupContainer("archiveHistoryTable") {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return !archives.isEmpty();
			}
		};

		add(table);

		// archive data view
		final PageableListView<Archive> listView = new PageableListView<Archive>("archiveHistoryView", archives, 10) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(final ListItem<Archive> item) {
				final Archive archive = item.getModelObject();

				log.debug(archive.toString());

				item.add(new Label("dateCompleted", Dateifier.toIso8601(archive.getEndDate())));
				item.add(new Label("status", archive.getStatus().toString()));
				item.add(new Label("creator", ShowArchives.this.businessService.getUserDisplayName(archive.getUserUuid())));

				// download link
				final DownloadLink downloadLink = new DownloadLink("downloadLink", getFileModel(archive), getFileName(archive)) {
					private static final long serialVersionUID = 1L;

					@Override
					public boolean isVisible() {
						return (archive.getStatus() == Status.COMPLETE);
					}

				};
				item.add(downloadLink);

				// highlight archives that are in progress
				if (archive.getStatus() == Status.STARTED) {
					item.add(AttributeModifier.append("class", "warning"));
				}
			}

		};
		listView.setReuseItems(true);
		table.add(new PagingNavigator("navigator", listView));
		table.add(listView);

		// no archives message
		add(new WebMarkupContainer("noArchives") {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return archives.isEmpty();
			}
		});
	}

	/**
	 * Get the file for the archive. Delegated to a {@link LoadableDetachableModel} so the file can be requested only when necessary.
	 *
	 * Note that this throws a {@link RuntimeException} which then takes us to the error page.
	 *
	 * @param archive
	 * @return
	 */
	private IModel<File> getFileModel(final Archive archive) {

		return new LoadableDetachableModel<File>() {

			private static final long serialVersionUID = 1L;

			@Override
			protected File load() {
				try {
					return ShowArchives.this.businessService.getArchiveZip(archive);
				} catch (final ArchiveNotFoundException e) {
					throw new ZipNotFoundException(getString("archive.error.notfound"));
				}
			}
		};
	}

	/**
	 * Get name of file
	 *
	 * @param archive
	 * @return name of file based on zip file name or null if no name
	 */
	private String getFileName(final Archive archive) {
		final String zipPath = archive.getZipPath();

		if (StringUtils.isNotBlank(zipPath)) {
			return FilenameUtils.getName(archive.getZipPath());
		}
		return null;
	}
}
