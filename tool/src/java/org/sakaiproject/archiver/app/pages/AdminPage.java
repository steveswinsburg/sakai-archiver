package org.sakaiproject.archiver.app.pages;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.ContextRelativeResource;
import org.sakaiproject.archiver.app.model.ArchiveSite;
import org.sakaiproject.archiver.app.panels.ShowArchives;
import org.wicketstuff.objectautocomplete.AutoCompletionChoicesProvider;
import org.wicketstuff.objectautocomplete.ObjectAutoCompleteBuilder;
import org.wicketstuff.objectautocomplete.ObjectAutoCompleteField;
import org.wicketstuff.objectautocomplete.ObjectAutoCompleteRenderer;

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

		final Form<Void> form = new Form<Void>("form");

		// AC builder
		final ObjectAutoCompleteBuilder<ArchiveSite, String> builder = new ObjectAutoCompleteBuilder<>(
				new AutoCompletionChoicesProvider<ArchiveSite>() {
					private static final long serialVersionUID = 1L;

					@Override
					public Iterator<ArchiveSite> getChoices(final String input) {
						final List<ArchiveSite> rval = new ArrayList<>();
						rval.add(new ArchiveSite("siteId1", "title1"));
						rval.add(new ArchiveSite("siteId2", "title2"));

						/*
						 * for (Car car : CarRepository.allCars()) { if (car.getName().toLowerCase().startsWith(input.toLowerCase())) {
						 * ret.add(car); } }
						 */
						return rval.iterator();
					}
				});

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

		// AC field
		final ObjectAutoCompleteField<ArchiveSite, String> search = builder.build("search", new Model<String>());
		form.add(search);

		add(form);

		// show recent archives across all sites (100)
		add(new ShowArchives("recentArchives", 100));

	}

}
