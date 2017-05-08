package org.sakaiproject.archiver.app.pages;

import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.sakaiproject.archiver.app.model.ArchiveSettings;
import org.sakaiproject.archiver.app.model.ArchiveableTool;
import org.sakaiproject.archiver.exception.ArchiveAlreadyInProgressException;
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

		// form model
		final ArchiveSettings settings = new ArchiveSettings();
		final Model<ArchiveSettings> formModel = Model.of(settings);

		// get tools configured for archiving
		final List<ArchiveableTool> configuredTools = this.businessService.getConfiguredTools();
		settings.setArchiveableTools(configuredTools);

		// form
		final Form<ArchiveSettings> form = new Form<ArchiveSettings>("create", formModel) {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return !configuredTools.isEmpty();
			}

			@Override
			public void onSubmit() {
				final ArchiveSettings formSettings = getModelObject();

				try {
					CreateArchivePage.this.businessService.createArchive(formSettings);
					success(getString("archive.started"));
				} catch (final ToolsNotSpecifiedException e) {
					error(getString("archive.error.notools"));
				} catch (final ArchiveAlreadyInProgressException e) {
					error(getString("archive.error.existing"));
				} catch (final ArchiveInitialisationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

		};

		// tool list
		final ListView<ArchiveableTool> includeToolsView = new ListView<ArchiveableTool>("includeToolsView", configuredTools) {
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
		final CheckBox includeStudentData = new CheckBox("includeStudentData", new PropertyModel<Boolean>(settings, "includeStudentData"));
		includeStudentData.setOutputMarkupId(true);
		form.add(includeStudentData);

		add(form);

		// error message if no tools
		final Label noToolsError = new Label("noToolsError", new ResourceModel("error.create.notools")) {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return configuredTools.isEmpty();
			}

		};
		add(noToolsError);

	}

}
