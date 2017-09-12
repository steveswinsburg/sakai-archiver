# sakai-archiver (for developers)

## Adding an archiver provider

Your service must implement `org.sakaiproject.archiver.spi.Archiveable`.

You will need to add the dependency:
````
<dependency>
  <groupId>org.sakaiproject.archiver</groupId>
  <artifactId>archiver-api</artifactId>
  <version>0.2-SNAPSHOT</version>
  <scope>provided</scope>
</dependency>
````

Then, to register your service, call `org.sakaiproject.archiver.api.ArchiveRegistry.getInstance().register(yourservice, toolId)`
when your service starts up. The init() block is a good place for this registration.

*Note:* If the init block does not exist in your service, find the spring bean XML and add `init-method="init"`, then create a `public void init()` method.

The `toolId` indicates which tool that this service will handle the archiving for. Multiple tools can be handled with a single service.

For example:
````
org.sakaiproject.archiver.api.ArchiverRegistry.getInstance().register("sakai.gradebookng", this);
org.sakaiproject.archiver.api.ArchiverRegistry.getInstance().register("sakai.gradebook.tool", this);
````

When an archive is initiated, your service's `archive()` implementation will be called for each toolId.

Use the methods in `org.sakaiproject.archiver.api.ArchiverService` within your service to send the data from your service to the archiver.

There is are helper utilities in the `api.util` package to assist with turning objects into JSON, HTML and dealing with dates.

## Custom builds
Some archivers may require services not available to your development environment. To include mocks of these in your deployment use `-Pinclude-mock-services`. Likewise if you need to create a mock service, ensure it is wrapped in this profile inside the pom.xml.

To include the custom providers, use `-Pinclude-custom`.

## 10.4 compatibility
Use the `compat/10.4` branch, and note the following:

* All patches in the `compat` directory must be applied to your base Sakai code before building.
* The compat/10.4 branch must be built with `-Pcompat`.
    Note that this will also deploy new/updated versions of some libraries to Tomcat. You *must* ensure the following libraries are removed from Tomcat's shared classloader:
        * commons-io-2.0.1.jar

## CI support
Since CI servers start with a clean slate for each build, the CI server has to include `-Psnapshots`.

## Database indexes
Indexes should be automatically created on versions of Sakai that can use JPA annotaitons (ie Sakai 11+), but for older versions of Sakai, run the following:
`CREATE INDEX site_id_idx ON archiver_archives (site_id)`
