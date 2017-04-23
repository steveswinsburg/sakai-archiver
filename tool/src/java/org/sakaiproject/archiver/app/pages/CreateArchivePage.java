package org.sakaiproject.archiver.app.pages;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
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

		// submit button
		final AjaxButton submit = new AjaxButton("submit", form) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onSubmit(final AjaxRequestTarget target, final Form<?> f) {
				final ArchiveSettings formSettings = (ArchiveSettings) f.getModelObject();
				System.out.println(formSettings);
			}

		};
		form.add(submit);
		add(form);
		
		//error message if no tools
		Label noToolsError = new Label("noToolsError", new ResourceModel("error.create.notools")) {
			private static final long serialVersionUID = 1L;
			
			@Override
			public boolean isVisible() {
				return configuredTools.isEmpty();
			}
			
		};
		add(noToolsError);

		
	}

}
