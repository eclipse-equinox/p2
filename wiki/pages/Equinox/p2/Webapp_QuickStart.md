## Quickstart - Using Equinox Provisioning in a Web Application

The Equinox provisioning work has in the past been used to demonstrate
its use deploying rich client applications however it’s equally relevant
for server-side applications. This Quickstart will provide a WAR file
that is an amalgamation of the servletbridge and new provisioning work.
The servletbridge handles the launching of Equinox and provides a
conduit for HTTP requests but is otherwise not involved in the
management of bundles running in an installation. As compared with the
current release (3.3) the way we’re going to manage the installation of
bundles is significantly different and we'll demonstrate the new
provisioning work by installing new functionality into an exisiting
server application.

## What you’ll need….

1\) A servlet container (I suggest the latest version of either Tomcat
or Jetty). We’re going to be using the OSGi console so it would simplify
things if you can use StdIn/Out. If not you will have to tweak the
web.xml to use a remote port.

2\) Provisioning WAR file --
[provbridge.war](http://eclipse.org/equinox/server/downloads/provbridge.war)
In Eclipse 3.3, the update configurator bundle
(org.eclipse.equinox.update.configurator) would traverse your
eclipse/plugins directory and install all bundles it found. For this
quickstart we’re going to do things a little differently. Rather than
just installing everything the provisioning work will generate a
metadata repository to represent the various requirements and
capabilities of each bundle.

3\) JSP Sample Repository WAR file --
[metajsp.war](http://eclipse.org/equinox/server/downloads/metajsp.war)
For the purposes of this quickstart we’re going to install this WAR file
in the same Servlet container however it’s meant to represent a remote
metadata repository similar to an Eclipse Update site.

## Instructions…

1\) Install the two WAR files in your Servlet container and start it up.
(e.g. in Tomcat place copy the two WAR files to your webapps directory
and run bin/startup.bat or bin/startup.sh)

2\) The OSGi console should start automatically and you should see the
OSGi prompt (e.g. “osgi\>”). At the prompt type “ss” to see the list of
the bundles that makes up our base provisioning agent.

Note: You might notice the org.eclipse.equinox.prov.selfgenerator is
STARTING. This bundle produces the initial metadata repository and
profile that describes the contents of the plugins folder and the
software currently installed. After it’s STARTED we’re ready to proceed.
You should see the following status message **"Status OK:
org.eclipse.equinox.prov code=0 null"**

3\) Let’s add a metadata and artifact repository so we can provision
some new functionality Type “?” to list the console commands. You should
see a number of the new provisioning commands.

Type the following in the console:

  - **provaddartifactrepo
    <http://localhost:8080/metajsp/artifactRepository/artifacts.xml>**
  - **provaddrepo <http://localhost:8080/metajsp/metadataRepository/>**

You can use the “provlr” and “provlar” commands to examine the contents
of the repositories if you like.

  - **provlr <http://localhost:8080/metajsp/metadataRepository/>**

4\) The repositories contain the metadata for adding JSP support and a
simple helloworld example. Let’s install into our current profile.

First let's examine our current profile. The IUs prefixed with "self"
are called IUFragments and provide configuration information for there
like name IU host.

  - **provlp eclipse.prov.profile.current**
      - *This displays nothing under Jetty 6.1.9 and following the above
        - Alex*

Next let's install our new functionality and take a look at what's
changed

  - **provinstall sample.jsp 1.0.0 eclipse.prov.profile.current**
      - *Doesn't work either - Alex*
      - *provinstall sample.jsp 1.0.0 eclipse.prov.profile.current*
      - *java.lang.IllegalArgumentException: URI has an authority
        component*
      - *at java.io.File.<init>(File.java:340)*
  - **provlp eclipse.prov.profile.current**
      - *Same as above Apr08*

What you should see is that in addition to sample.jsp all of it's
dependencies have been added. In this case javax.servlet.jsp the jasper
engine and it's dependencies.

5\) The change is in our current profile now, so let's enable the
change.

  - **confapply**

6\) Type **ss** to view the newly installed JSP bundles. You can now
visit <http://localhost:8080/provbridge/helloworld.jsp> to see the JSP
page you’ve just provisioned.

[Webapp Quick start](Category:Equinox_p2 "wikilink")