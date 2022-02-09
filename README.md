# Digital Transfer Tool

[![Build Status](https://travis-ci.com/MITLibraries/att.svg?branch=master)](https://travis-ci.com/MITLibraries/att)

The application enables secure and reliable transfer of records, archives, research data, or other digital content to Libraries’
Collections staff as a step in the digital curation workflow. There is currently no web-based tool available at MIT to 
donors or creators of digital material that they can use to securely and reliably transfer digital files and 
metadata to the Libraries in a consistent and agreed upon way and that aligns with the PAIMAS standard and PAIS protocol.

Software Requirements
----------------------

Docker or Apache Maven


Installation locally (using Maven)
----------------------------------

This is a vanilla Maven based project. Install Maven, and then..

```sh

# from the folder, run the build, and package it:

mvn -Dspring.profiles.active=dev clean package -P dev

# after building, test it:

java -jar target/att-0.0.1-SNAPSHOT.war

```

Visit `http://localhost:8080/att`.




Deployment (on Production)
-------------------------------

(See also design notes on infrastrure set up.)

For production, we need to run the .war with application-prod.properties, where the connection
to MySql and the path to file share is specified. Most of the settings are already there (except the password, which is read from 
a config file on the stage and production server itself.)

Adjust application-prod.properties (in `resources` folder) to:

- point to the correct share directory on the server (where files are submitted). The paths are slightly different on 
  stage and prod (see application-prod.properties for the exact path)
- ensure tomcat setenv.sh has the password set and Spring profile as active (see below).

Build using Maven on your machine as follows, and copy the resulting .war file to the Tomcat instance:

```sh

# from the folder, run the build, and copy the file to the correct tomcat location

mvn clean package -P prod

# scp target/att-0.0.1-SNAPSHOT.war user@server:/opt/tomcat/webapps/att.war

```

Note: Replace user, server, and tomcat webapp path, as appropriate. Also ensure that Tomcat's setenv.sh in Tomcat (bin folder) 
has this line:

```shell
export SPRING_PROFILES_ACTIVE=prod
```

Tomcat will load the web app after a few moments. 

To debug, you can `tail` the following files:

``` sh
 sudo tail -f /opt/tomcat/logs/catalina.out
 sudo tail -f /opt/tomcat/logs/localhost.yyyy-mm-dd.log
```

Again, change Tomcat location (from `/opt/tomcat`, for example), as necessary.

Hit server:8080/att to see the app in action.
The application logs folder can be changed in .properties.

Docker testing
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

Single unit test running
-----------

To run a single test:

```sh 
mvn surefire:test -Dtest=DepartmentHttpRequestTest#testAddPage -Pdev
```

Architecture
---------------------

Currently, the application is running on https:arcsubmit mit.edu/att and arcsubmit-stage.

Server set up: 
- Apache httpd (with Shibboleth)
- Apache Tomcat
- MySql
- JDK

Note: It is installed on a local VM because it needs to mount to a local share for confidential content (cannot be random S3).
Do NOT migrate to cloud without discussing the implications with business stakeholders.

Documentation
--------------

The app's FAQ web page provides background information on the submission process.


Contributors
-------------

The project was developed by Osman Din and Frances Botsford. 


Acknowledgements
-----------------
The application is based on a tool developed by the 
Michigan State University.



Project Management
------------------

If you encounter an issue, please file a bug on GitHub (or MIT's JIRA website, if you are an internal user):

https://mitlibraries.atlassian.net/projects/ATT

License
-------

This project is licensed under AGPL. See the license file for details.