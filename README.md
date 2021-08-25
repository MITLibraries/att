# Digital Transfer Tool

[![Build Status](https://travis-ci.com/MITLibraries/att.svg?branch=master)](https://travis-ci.com/MITLibraries/att)

The application enables secure and reliable transfer of records, archives, research data, or other digital content to Libraries’
Collections staff as a step in the digital curation workflow. There is currently no web-based tool available at MIT to 
donors or creators of digital material that they can use to securely and reliably transfer digital files and 
metadata to the Libraries in a consistent and agreed upon way and that aligns with the PAIMAS standard and PAIS protocol.

Software Requirements
----------------------

Docker or Apache Maven


Installation (Maven)
----------------------

This is a Maven based project. Assuming you've installed Maven...

```sh

# from the folder, run the build, and package it:

mvn -Dspring.profiles.active=dev clean package -P dev

# after building, test it:

java -jar target/att-0.0.1-SNAPSHOT.war

```

Visit `http://localhost:8080/att`.

Testing with Docker
-----------------------

For convenience, the project can be launched with Docker

```sh
# first time only:
docker pull maven:3.5.3-jdk-8-alpine

# from the folder, build the docker image (it will take a while the first time):

docker build -t att:1.0 .

# now run it:

docker run -p 8080:8080 att:1.0

# the app should now be live at localhost:8080/att
 
```

Now if you want to make a change to the app:

- Hit ```Ctrl-C```
- Edit /src/main/resources/templates/AddDepartment.html (for example)
- Run the image build command (```docker build```), as described above (it should take a second now).
- Run the image again (```docker run```).


Deployment (Production)
-------------------------------

Adjust application.properties to:

- point to the correct share directory on the server.
- (optional: update with SMTP password)

Build using Maven and copy the result .war file to the Tomcat instance:

```sh

# from the folder, run the build, and package it:

mvn -Dspring.profiles.active=prod clean package -P prod

# scp target/att-0.0.1-SNAPSHOT.war user@server:/usr/share/tomcat/webapps

```

Note: Replace user, server, and tomcat webapp path, as appropriate.

Tomcat should load the web app after a few moments. Browse to the webapp (specified below)
to confirm the application is working.

To debug, you can tail the following files:

``` sh
 sudo tail -f /opt/tomcat/logs/catalina.out
 sudo tail -f /usr/share/tomcat/logs/localhost.yyyy-mm-dd.log
```

Change Tomcat location, as necessary.

The application logs folder can be changed in application.properties.

Unit Tests
-----------

To run a single test:

```sh 
mvn surefire:test -Dtest=DepartmentHttpRequestTest#testAddPage -Pdev
```

Design
---------------------

Currently, the application is running on https:arcsubmit mit.edu/att and arcsubmit-stage.

Server set up: Apache httpd (for Shibboleth), Apache Tomcat, MySql

Documentation
--------------

The app's FAQ web page provides background information on the submission process.


Contributors
-------------

The project was developed by Osman Din and Frances Botsford. 


Acknowledgements
-----------------
It is based on a tool developed by the 
Michigan State University.



Project Management
------------------

If you encounter an issue, please file a bug on GitHub (or MIT's JIRA website, if you are an internal user):

https://mitlibraries.atlassian.net/projects/ATT

License
-------

This project is licensed under AGPL. See the license file for details.