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

	@Before
	public void clearRegistry() {
		final ArchiverRegistry registry = ArchiverRegistry.getInstance();
		registry.getRegistry().keySet().forEach(toolId -> registry.unregister(toolId));
	}

	@Test
	public void should_register_when_archiveableSupplied() {
		final ArchiverRegistry registry = ArchiverRegistry.getInstance();

		final Archiveable archiveable1 = new ExampleArchiveable();
		registry.register(this.toolId1, archiveable1);

		final List<Archiveable> registered = registry.getRegistry().get(this.toolId1);

		Assert.assertThat("Archiveables are not identical", registered.get(0), samePropertyValuesAs(archiveable1));
	}

	@Test
	public void should_supplement_when_archiveableSuppliedWithSameToolId() {
		final ArchiverRegistry registry = ArchiverRegistry.getInstance();

		final Archiveable archiveable1 = new ExampleArchiveable();
		registry.register(this.toolId1, archiveable1);

		final Archiveable archiveable2 = new ExampleArchiveable();
		registry.register(this.toolId1, archiveable2);

		final Archiveable archiveable3 = new ExampleArchiveable();
		registry.register(this.toolId1, archiveable3);

		final List<Archiveable> registered = registry.getRegistry().get(this.toolId1);

		Assert.assertThat("Archiveables are not identical", registered.get(0), samePropertyValuesAs(archiveable1));
		Assert.assertThat("Archiveables are not identical", registered.get(1), samePropertyValuesAs(archiveable2));
		Assert.assertThat("Archiveables are not identical", registered.get(2), samePropertyValuesAs(archiveable3));

	}

	@Test
	public void should_unregister_when_toolIdSupplied() {
		final ArchiverRegistry registry = ArchiverRegistry.getInstance();

		final ExampleArchiveable archiveable1 = new ExampleArchiveable();
		registry.register(this.toolId1, archiveable1);
		registry.unregister(this.toolId1);

		final ExampleArchiveable registered = (ExampleArchiveable) registry.getRegistry().get(this.toolId1);

		Assert.assertNull("Archiveable was not unregistered", registered);
	}

	protected class ExampleArchiveable implements Archiveable {

		public ExampleArchiveable() {
		}

		@Override
		public void archive(final String archiveId, final String siteId, final boolean includeStudentContent) {
			// do nothing
		}

		@Override
		public String getToolName(final String siteId) {
			return "";
		}

	}

}
