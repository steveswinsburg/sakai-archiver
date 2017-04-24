# sakai-archiver (for developers)

## Adding a service


Your service must implement `org.sakai.project.archiver.api.Archiveable`.

Then, to register your service, call `org.sakai.project.archiver.api.ArchiveRegistry.getInstance().register(yourservice)`
when your service starts up. The init() block is a good place for this registration.

When an archive is initiated, your service's `archive` implementation will be called.

Use the methods in `org.sakai.project.archiver.api.ArchiverService` within your service to send the data from your service to the archiver.


