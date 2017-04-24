# sakai-archiver for developers

## Registering a service

Your service must implement `org.sakai.project.archiver.api.Archiveable`.

To register your service, call `org.sakai.project.archiver.api.ArchiveRegistry.getInstance().register(yourservice);`.

When an archive is initiated, your service will be called.

Use the methods in `org.sakai.project.archiver.api.ArchiverService` within your service to send the data from your service to the archiver.


