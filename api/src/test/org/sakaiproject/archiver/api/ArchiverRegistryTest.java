package org.sakaiproject.archiver.api;

import static org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.archiver.spi.Archiveable;

public class ArchiverRegistryTest {

	String toolId1 = "my.tool.1";
	String toolId2 = "my.tool.2";
	String toolId3 = "my.tool.3";
	String name1 = "name1";
	String name2 = "name2";

	@Before
	public void clearRegistry() {
		ArchiverRegistry.getInstance().unregisterAll();
	}

	@Test
	public void should_register_when_archiveableSupplied() {
		final ArchiverRegistry registry = ArchiverRegistry.getInstance();

		final Archiveable archiveable1 = new ExampleArchiveable(this.toolId1, this.name1);
		registry.register(archiveable1);

		final List<Archiveable> registered = registry.getRegistry().get(this.toolId1);

		Assert.assertThat("Archiveables are not identical", registered.get(0), samePropertyValuesAs(archiveable1));
	}

	@Test
	public void should_supplement_when_archiveableSuppliedWithSameToolId() {
		final ArchiverRegistry registry = ArchiverRegistry.getInstance();

		final Archiveable archiveable1 = new ExampleArchiveable(this.toolId1, this.name1);
		registry.register(archiveable1);

		final Archiveable archiveable2 = new ExampleArchiveable(this.toolId1, this.name1);
		registry.register(archiveable2);

		final Archiveable archiveable3 = new ExampleArchiveable(this.toolId1, this.name2);
		registry.register(archiveable3);

		final List<Archiveable> registered = registry.getRegistry().get(this.toolId1);

		Assert.assertThat("Archiveables are not identical", registered.get(0), samePropertyValuesAs(archiveable1));
		Assert.assertThat("Archiveables are not identical", registered.get(1), samePropertyValuesAs(archiveable2));
		Assert.assertThat("Archiveables are not identical", registered.get(2), samePropertyValuesAs(archiveable3));

	}

	@Test
	public void should_unregister_when_toolIdSupplied() {
		final ArchiverRegistry registry = ArchiverRegistry.getInstance();

		final ExampleArchiveable archiveable1 = new ExampleArchiveable(this.toolId1, this.name1);
		registry.register(archiveable1);
		registry.unregister(this.toolId1);

		final ExampleArchiveable registered = (ExampleArchiveable) registry.getRegistry().get(this.toolId1);

		Assert.assertNull("Archiveable was not unregistered", registered);
	}

	protected class ExampleArchiveable implements Archiveable {

		private final String name;
		private final String toolId;

		public ExampleArchiveable(final String toolId, final String name) {
			this.toolId = toolId;
			this.name = name;
		}

		@Override
		public void archive(final String archiveId, final String siteId, final boolean includeStudentContent) {
			// do nothing
		}

		@Override
		public String getToolId() {
			return this.toolId;
		}

		@Override
		public String getName() {
			return this.name;
		}

	}

}
