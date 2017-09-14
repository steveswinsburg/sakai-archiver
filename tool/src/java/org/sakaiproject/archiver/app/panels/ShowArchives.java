package org.sakaiproject.archiver.app.panels;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.archiver.api.Status;
import org.sakaiproject.archiver.app.business.ArchiverBusinessService;
import org.sakaiproject.archiver.app.components.RichFeedbackPanel;
import org.sakaiproject.archiver.dto.Archive;
import org.sakaiproject.archiver.exception.ArchiveCancellationException;
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
	private final int max;
	private final boolean showSite;

	@SpringBean(name = "org.sakaiproject.archiver.app.business.ArchiverBusinessService")
	private transient ArchiverBusinessService businessService;

	List<Archive> archives;

	/**
	 * Show archives for current site
	 *
	 * @param id markupid
	 */
	public ShowArchives(final String id) {
		super(id);
		this.siteId = this.businessService.getCurrentSiteId();
		this.max = 50;
		this.showSite = false;
	}

	/**
	 * Show archives for given site
	 *
	 * @param id markupid
	 * @param model the siteId wrapped in a Model
	 */
	public ShowArchives(final String id, final IModel<String> model) {
		super(id, model);
		this.siteId = model.getObject();
		this.max = 50;
		this.showSite = false;
	}

	/**
	 * Show archives for all sites up to maximum Typically used by admin views.
	 *
	 * @param id markupid
	 * @param max the maximum to show
	 */
	public ShowArchives(final String id, final int max) {
		super(id);
		this.siteId = null;
		this.max = max;
		this.showSite = true;
	}

	@Override
	public void onInitialize() {
		super.onInitialize();

		log.debug("Archives for: " + this.siteId);

		// get list of archives
		getArchives();

		log.debug("Count: " + this.archives.size());

		// wrap the table
		final WebMarkupContainer table = new WebMarkupContainer("archiveHistoryTable") {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return !ShowArchives.this.archives.isEmpty();
			}
		};
		table.setOutputMarkupId(true);
		add(table);

		// this header is only shown if showSite is true
		table.add(new WebMarkupContainer("siteHeader") {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return ShowArchives.this.showSite;
			}
		});

		// archive data view, wrapped and getData overriden so it can use whatever data we have
		final ListDataProvider<Archive> listDataProvider = new ListDataProvider<Archive>() {
			private static final long serialVersionUID = 1L;

			@Override
			protected List<Archive> getData() {
				return ShowArchives.this.archives;
			}
		};

		final DataView<Archive> view = new DataView<Archive>("archiveHistoryView", listDataProvider) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(final Item<Archive> item) {
				final Archive archive = item.getModelObject();

				log.debug(archive.toString());

				// this column is only shown if showSite is true
				item.add(new Label("site", ShowArchives.this.businessService.getSiteTitle(archive.getSiteId())) {
					private static final long serialVersionUID = 1L;

					@Override
					public boolean isVisible() {
						return ShowArchives.this.showSite;
					}
				});

				item.add(new Label("dateCompleted", Dateifier.toIso8601(archive.getEndDate())));

				final WebMarkupContainer status = new WebMarkupContainer("status");
				status.add(new Label("statusText", archive.getStatus().toString()));
				status.add(new AjaxLink<Void>("cancelLink") {

					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(final AjaxRequestTarget target) {
						try {
							ShowArchives.this.businessService.cancelArchive(archive.getArchiveId());
							getArchives();
						} catch (final ArchiveCancellationException e) {
							error(getString("archive.error.cancel"));
							target.addChildren(getPage(), RichFeedbackPanel.class);
						}
						target.add(table);

					}

					@Override
					public boolean isVisible() {
						return (archive.getStatus() == Status.STARTED);
					}

				});
				status.setOutputMarkupId(true);
				item.add(status);

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
		view.setItemsPerPage(10);
		table.add(new PagingNavigator("navigator", view));
		table.add(view);

		// no archives message
		add(new WebMarkupContainer("noArchives") {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return ShowArchives.this.archives.isEmpty();
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

	/**
	 * Convenience method to get the list of archives
	 *
	 * @return
	 */
	private void getArchives() {
		this.archives = this.businessService.getArchives(this.siteId, this.max);
	}

}
