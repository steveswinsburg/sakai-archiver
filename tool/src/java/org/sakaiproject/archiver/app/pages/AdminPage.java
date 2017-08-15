package org.sakaiproject.archiver.app.pages;

import java.util.Iterator;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.ContextRelativeResource;
import org.sakaiproject.archiver.app.model.ArchiveSite;
import org.sakaiproject.archiver.app.panels.ShowArchives;
import org.wicketstuff.objectautocomplete.AutoCompletionChoicesProvider;
import org.wicketstuff.objectautocomplete.ObjectAutoCompleteBuilder;
import org.wicketstuff.objectautocomplete.ObjectAutoCompleteField;
import org.wicketstuff.objectautocomplete.ObjectAutoCompleteRenderer;
import org.wicketstuff.objectautocomplete.ObjectAutoCompleteSelectionChangeListener;

/**
 * Admin view
 */
public class AdminPage extends BasePage {

	private static final long serialVersionUID = 1L;

	Panel siteArchives;

	public AdminPage() {
		disableLink(this.adminLink);
	}

	@Override
	public void onInitialize() {
		super.onInitialize();

		final ArchiveSite siteModel = new ArchiveSite();
		final Form<ArchiveSite> form = new Form<ArchiveSite>("form", Model.of(siteModel));

		// AC builder
		final ObjectAutoCompleteBuilder<ArchiveSite, String> builder = new ObjectAutoCompleteBuilder<>(
				new AutoCompletionChoicesProvider<ArchiveSite>() {
					private static final long serialVersionUID = 1L;

					@Override
					public Iterator<ArchiveSite> getChoices(final String input) {
						final List<ArchiveSite> rval = AdminPage.this.businessService.findSites(input);
						return rval.iterator();
					}
				});

		// change listener
		final ObjectAutoCompleteSelectionChangeListener<String> listener = new ObjectAutoCompleteSelectionChangeListener<String>() {
			private static final long serialVersionUID = 1L;

			@Override
			public void selectionChanged(final AjaxRequestTarget target, final IModel<String> model) {
				final Panel newPanel = new ShowArchives("siteArchives", model);
				newPanel.setOutputMarkupId(true);
				AdminPage.this.siteArchives.replaceWith(newPanel);
				AdminPage.this.siteArchives = newPanel; // keep reference up to date!
				target.add(newPanel);
			}
		};
		builder.updateOnSelectionChange(listener).idType(String.class);

		// AC renderer
		final ObjectAutoCompleteRenderer<ArchiveSite> renderer = new ObjectAutoCompleteRenderer<ArchiveSite>() {
			private static final long serialVersionUID = 1L;

			@Override
			protected String getIdValue(final ArchiveSite a) {
				return a.getSiteId();
			}

			@Override
			protected String getTextValue(final ArchiveSite a) {
				return a.getTitle();
			}
		};
		builder.autoCompleteRenderer(renderer);
		builder.searchLinkImage(new ContextRelativeResource("images/cross.png"));
		builder.preselect();

		// AC field
		final ObjectAutoCompleteField<ArchiveSite, String> searchField = builder.build("search",
				new PropertyModel<String>(siteModel, "siteId"));

		/*
		 * searchField.add(new AjaxFormSubmitBehavior("onchange") { private static final long serialVersionUID = 1L;
		 *
		 * @Override protected final void onSubmit(final AjaxRequestTarget target) { final ArchiveSite model = form.getModelObject();
		 * System.out.println(model);
		 *
		 * final Panel newPanel = new ShowArchives("recentArchives", 100); newPanel.setOutputMarkupId(true);
		 * AdminPage.this.siteArchives.replaceWith(newPanel); AdminPage.this.siteArchives = newPanel; // keep reference up to date!
		 *
		 * target.add(newPanel); } });
		 */

		form.add(searchField);
		add(form);

		this.siteArchives = new EmptyPanel("siteArchives");
		this.siteArchives.setOutputMarkupPlaceholderTag(true);
		add(this.siteArchives);

		// show recent archives across all sites (100)
		add(new ShowArchives("recentArchives", 100));

	}

}
