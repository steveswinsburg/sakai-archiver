# sakai-archiver (for developers)

## Adding a service

Your service must implement `org.sakaiproject.archiver.api.Archiveable`.

You will need to add the dependency:
````
<dependency>
  <groupId>org.sakaiproject.archiver</groupId>
  <artifactId>archiver-api</artifactId>
  <version>0.1-SNAPSHOT</version>
  <scope>provided</scope>
</dependency>
````

Then, to register your service, call `org.sakai.project.archiver.api.ArchiveRegistry.getInstance().register(yourservice, toolId)`
when your service starts up. The init() block is a good place for this registration.

*Note:* If the init block does not exist in your service, find the spring bean XML and add `init-method="init"`, then create a `public void init()` method.

The `toolId` indicates which tool that this service will handle the archiving for. Multiple tools could be handled with the one service.

For example:
````
org.sakaiproject.archiver.api.ArchiverRegistry.getInstance().register("sakai.gradebookng", this);
org.sakaiproject.archiver.api.ArchiverRegistry.getInstance().register("sakai.gradebook.tool", this);
````

When an archive is initiated, your service's `archive()` implementation will be called for each toolId.

Use the methods in `org.sakai.project.archiver.api.ArchiverService` within your service to send the data from your service to the archiver.


