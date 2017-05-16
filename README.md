# sakai-archiver
An archiving app for Sakai

## Configuration

In `akai.properties`, set the list of `archiver.tools` and the `archiver.path` which is where archives will be created. 

For example:

````
archiver.tools=sakai.syllabus,sakai.gradebook.tool,sakai.gradebookng,sakai.resources,sakai.assignment.grades,sakai.forums,sakai.dropbox,sakai.announcements,sakai.siteinfo,sakai.postem,sakai.iframe.annotatedurl,sakai.chat,sakai.samigo.tool
archiver.path = /Users/steve/sakai/archives/
````

## Developers
See ![DEVELOPERS.md](DEVELOPERS.md)

## Status
[![Build status](https://travis-ci.org/steveswinsburg/sakai-archiver.svg?branch=master)](https://travis-ci.org/steveswinsburg/sakai-archiver) 
[![Quality Gate](https://sonarqube.com/api/badges/gate?key=org.sakaiproject.archiver:archiver)](https://sonarqube.com/dashboard/index/org.sakaiproject.archiver:archiver)

---
Development sponsored by Yale University (www.yale.edu)
