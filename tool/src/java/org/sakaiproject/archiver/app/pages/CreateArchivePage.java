package org.sakaiproject.archiver.app.pages;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.time.Duration;
import org.sakaiproject.archiver.app.model.ArchiveSettings;
import org.sakaiproject.archiver.app.model.ArchiveableTool;
import org.sakaiproject.archiver.exception.ArchiveAlreadyInProgressException;
import org.sakaiproject.archiver.exception.ArchiveCompletionException;
import org.sakaiproject.archiver.exception.ArchiveInitialisationException;
import org.sakaiproject.archiver.exception.ToolsNotSpecifiedException;

public class CreateArchivePage extends BasePage {

	private static final long serialVersionUID = 1L;

	public CreateArchivePage() {
		disableLink(this.createArchiveLink);
	}

	@Override
	public void onInitialize() {
		super.onInitialize();

		// status banner
		final WebMarkupContainer statusContainer = new WebMarkupContainer("statusContainer");
		statusContainer.setOutputMarkupId(true);
		final Label statusBanner = new Label("statusBanner");
		statusBanner.setOutputMarkupId(true);
		statusContainer.add(statusBanner);
		add(statusContainer);

		// set initial state of the status banner in case an archive is currently running when they come to the page
		updateStatusBanner(statusBanner, getArchiveStatus());

		// form model
		final ArchiveSettings settings = new ArchiveSettings();
		final Model<ArchiveSettings> formModel = Model.of(settings);

		// get tools configured for archiving
		final List<ArchiveableTool> archiveableTools = this.businessService.getArchiveableTools();
		settings.setArchiveableTools(archiveableTools);

		// form
		final Form<ArchiveSettings> form = new Form<ArchiveSettings>("create", formModel) {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return !archiveableTools.isEmpty();
			}

			@Override
			public void onSubmit() {
				final ArchiveSettings formSettings = getModelObject();

				try {
					CreateArchivePage.this.businessService.createArchive(formSettings);
					updateStatusBanner(statusBanner, Status.IN_PROGRESS);
				} catch (final ToolsNotSpecifiedException e) {
					error(getString("archive.error.notools"));
				} catch (final ArchiveAlreadyInProgressException e) {
					// error(getString("archive.error.existing"));
				} catch (final ArchiveInitialisationException e) {
					error(getString("archive.error.starting"));
				} catch (final ArchiveCompletionException e) {
					error(getString("archive.error.completing"));
				}

			}

		};

		// tool list
		final ListView<ArchiveableTool> includeToolsView = new ListView<ArchiveableTool>("includeToolsView", archiveableTools) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(final ListItem<ArchiveableTool> item) {

				final ArchiveableTool tool = item.getModelObject();

				// include
				final CheckBox include = new CheckBox("include", new PropertyModel<Boolean>(tool, "includeInArchive"));
				include.setOutputMarkupId(true);
				item.add(include);

				// tool title
				final Label title = new Label("name", tool.getName());
				item.add(title);
			}
		};
		form.add(includeToolsView);

		// include student data
		// only if admin
		final CheckBox includeStudentData = new CheckBox("includeStudentData", new PropertyModel<Boolean>(settings, "includeStudentData")) {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return CreateArchivePage.this.businessService.isSuperUser();
			}
		};
		includeStudentData.setOutputMarkupId(true);
		form.add(includeStudentData);

		add(form);

		// error message if no tools
		final Label noToolsError = new Label("noToolsError", new ResourceModel("error.create.notools")) {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return archiveableTools.isEmpty();
			}

		};
		add(noToolsError);

		// archive status banner, refresh behaviour
		statusContainer.add(new AjaxSelfUpdatingTimerBehavior(Duration.seconds(2)) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onPostProcessTarget(final AjaxRequestTarget target) {
				final Status archiveStatus = getArchiveStatus();
				updateStatusBanner(statusBanner, archiveStatus);
			}
		});

	}

	/**
	 * Update status banner based on current status
	 *
	 * @param label
	 * @param status
	 */
	private void updateStatusBanner(final Label label, final Status status) {

		if (status == Status.NONE) {
			label.setVisible(false);
		}
		if (status == Status.IN_PROGRESS) {
			label.setDefaultModel(new ResourceModel("archive.status.inprogress"));
			label.add(AttributeModifier.replace("class", "messageSuccess"));
			label.setVisible(true);
		}
		if (status == Status.RECENTLY_COMPLETED) {
			label.setDefaultModel(new ResourceModel("archive.status.recentlycompleted"));
			label.add(AttributeModifier.replace("class", "messageWarning"));
			label.setVisible(true);
		}
		label.setEscapeModelStrings(false);
	}

	/**
	 * Get the status of the archive for this site, if any
	 *
	 * @return
	 */
	private Status getArchiveStatus() {
		if (this.businessService.isArchiveInProgress()) {
			return Status.IN_PROGRESS;
		} else if (this.businessService.isArchiveRecentlyCompleted()) {
			return Status.RECENTLY_COMPLETED;
		} else {
			return Status.NONE;
		}
	}

	/**
	 * Status for an archive so we can style accordingly
	 *
	 */
	private enum Status {
		IN_PROGRESS,
		RECENTLY_COMPLETED,
		NONE;
	}

}
