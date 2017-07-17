# sakai-archiver
An archiving app for Sakai which can be used as a snapshot of a course. 
Instructors can optionally choose to include all student generated content in the archive. 

## Configuration

In `sakai.properties`, set the list of `archiver.tools` and the `archiver.path` which is where archives will be created. 

For example:
```
archiver.tools=sakai.syllabus,sakai.gradebook.tool,sakai.gradebookng,sakai.resources,sakai.assignment.grades,sakai.forums,sakai.dropbox,sakai.announcements,sakai.iframe.site,sakai.postem,sakai.iframe.annotatedurl,sakai.chat,sakai.samigo.tool,sakai.simple.rss,sakai.site.roster2
archiver.path = /Users/steve/sakai/archives/
```

You can also optionally set the maximum filesize of a file included in the archive (in MB), and any file types to skip.

For example:
```
archiver.max.filesize=10
archiver.excluded.extensions=zip,gz,DS_Store
```

## Deployment
By default, all archiver providers are deployed. However, each archiver provider can be deployed independently if required. Simply build the ones you require or edit the base `pom.xml` and look for the `<modules>` section.

To include custom archivers, use `-Pinclude-custom`.

See also ![DEVELOPERS.md](DEVELOPERS.md) for more information on custom builds.

## Permissions

To access the archiver you must have the `site.upd` permission with in a site.

In addition, each provider requires full access to the data it is accessing. This means that the user initialing the import must have an instructor type permission in each tool. The specific permissions for each provider are:
<dl>
  <dt>Gradebook</dt>
  <dd>gradebook.gradeAll</dd>
  <dt>PostEm</dt>
  <dd>site.upd</dd>
</dl>

There are more. I will add soon :)

## Developers
See ![DEVELOPERS.md](DEVELOPERS.md)

## Status
[![Build status](https://travis-ci.org/steveswinsburg/sakai-archiver.svg?branch=master)](https://travis-ci.org/steveswinsburg/sakai-archiver) 

[![Quality Gate](https://sonarqube.com/api/badges/gate?key=org.sakaiproject.archiver:archiver)](https://sonarqube.com    /dashboard/index/org.sakaiproject.archiver:archiver)

---
Development sponsored by Yale University (www.yale.edu)
