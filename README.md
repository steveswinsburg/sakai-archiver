# sakai-archiver
An archiving app for Sakai

## Configuration

In `sakai.properties`, set the list of `archiver.tools` and the `archiver.path` which is where archives will be created. 

For example:
```
archiver.tools=sakai.syllabus,sakai.gradebook.tool,sakai.gradebookng,sakai.resources,sakai.assignment.grades,sakai.forums,sakai.dropbox,sakai.announcements,sakai.iframe.site,sakai.postem,sakai.iframe.annotatedurl,sakai.chat,sakai.samigo.tool
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

## Permissions
Each provider requires full access to the data it is accessing. This means that the user initialing the import must have an instructor type permission in each tool. The specific permissions for each provider are:
<dl>
  <dt>Gradebook</dt>
  <dd>gradebook.gradeAll</dd>
</dl>

## Developers
See ![DEVELOPERS.md](DEVELOPERS.md)

## Status
[![Build status](https://travis-ci.org/steveswinsburg/sakai-archiver.svg?branch=master)](https://travis-ci.org/steveswinsburg/sakai-archiver) 
[![Quality Gate](https://sonarqube.com/api/badges/gate?key=org.sakaiproject.archiver:archiver)](https://sonarqube.com/dashboard/index/org.sakaiproject.archiver:archiver)

---
Development sponsored by Yale University (www.yale.edu)
