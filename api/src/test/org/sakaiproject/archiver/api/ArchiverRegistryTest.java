package org.sakaiproject.archiver.api;

import static org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs;

import org.junit.Assert;
import org.junit.Test;

public class ArchiverRegistryTest {

	String toolId1 = "my.tool.1";
	String toolId2 = "my.tool.2";

	@Test
	public void should_register_when_archiveableSupplied() {
		final ArchiverRegistry registry = ArchiverRegistry.getInstance();

		final ExampleArchiveable archiveable1 = new ExampleArchiveable(1);
		registry.register(this.toolId1, archiveable1);

		final ExampleArchiveable registered = (ExampleArchiveable) registry.getRegistry().get(this.toolId1);

		Assert.assertThat("Archiveables are not identical", registered, samePropertyValuesAs(archiveable1));
	}

	@Test
	public void should_notRegisterAgain_when_archiveableAlreadyRegistered() {
		final ArchiverRegistry registry = ArchiverRegistry.getInstance();

		final ExampleArchiveable archiveable1 = new ExampleArchiveable(1);
		registry.register(this.toolId1, archiveable1);

		final ExampleArchiveable archiveable2 = new ExampleArchiveable(2);
		registry.register(this.toolId1, archiveable2);

		final ExampleArchiveable registered = (ExampleArchiveable) registry.getRegistry().get(this.toolId1);

		Assert.assertThat("Registry contained an overwritten impl", registered, samePropertyValuesAs(archiveable1));

	}

	@Test
	public void should_unregister_when_toolIdSpplied() {
		final ArchiverRegistry registry = ArchiverRegistry.getInstance();

		final ExampleArchiveable archiveable1 = new ExampleArchiveable(1);
		registry.register(this.toolId1, archiveable1);
		registry.unregister(this.toolId1);

		final ExampleArchiveable registered = (ExampleArchiveable) registry.getRegistry().get(this.toolId1);

		Assert.assertNull("Archiveable was not unregistered", registered);
	}

	protected class ExampleArchiveable implements Archiveable {

		// something to distinguish them
		private final long id;

		public ExampleArchiveable(final long id) {
			this.id = id;
		}

		@Override
		public void archive(final String archiveId, final String siteId, final boolean includeStudentContent) {
			// do nothing
		}

		public long getId() {
			return this.id;
		}

	}

}
